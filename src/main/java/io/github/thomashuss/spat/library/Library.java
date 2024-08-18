package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.Spat;
import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Stat;
import org.lmdbjava.Txn;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coherently tracks all library resources.
 */
/*
 * Saved -> Track -----┐
 *   |        🡙        ￬
 *   └----> Album -> Artist
 *            |        ￬
 *            └----> Genre
 *            └----> Label
 */
public class Library
        implements AutoCloseable
{
    public static final long INITIAL_MAP_SIZE = 100_485_760;
    private static final String LIKED_SONGS_KEY = "likedSongs";
    private static final String SAVED_ALBUMS_KEY = "savedAlbums";
    /**
     * Purely heuristic.
     */
    private static final int CHECK_SIZE_FREQ = 1000;
    private static final byte SHOULD_SAVE = 1;
    private static final byte SHOULD_SAVE_CONTENTS = 2;

    /**
     * No need for a thread safe instance due to synchronization on lmdb env.
     */
    private static final Fury fury = Spat.fury;

    static {
        fury.register(Album.class);
        fury.register(Artist.class);
        fury.register(AudioFeatures.class);
        fury.register(Genre.class);
        fury.register(Label.class);
        fury.register(Playlist.class);
        fury.register(SavedResourceCollection.class);
        fury.register(Temporal.class);
        fury.register(Track.class);
        fury.registerSerializer(URL.class, new URLSerializer(fury));
        fury.register(ZonedDateTime.class);
    }

    private final SaveDirectory state;
    private final int pageSize;
    private final Dbi<ByteBuffer> savedResourceListDb;
    private final Env<ByteBuffer> env;
    private final Queue<Runnable> needsFinalize;
    private final ResourceKV<Album> albumDb;
    private final ResourceKV<Artist> artistDb;
    private final ResourceKV<Genre> genreDb;
    private final ResourceKV<Label> labelDb;
    private final ResourceKV<Playlist> playlistDb;
    private final ResourceKV<Track> trackDb;
    private final Map<LibraryResource, Byte> needsSaveStatus;
    private final Queue<Runnable> needsSave;
    private final ByteBuffer keyBuf;
    private final MemoryBuffer keyMemBuf;
    private final ReferenceQueue<LibraryResource> rq;
    private ByteBuffer valBuf = ByteBuffer.allocateDirect(1024);
    private MemoryBuffer valMemBuf = MemoryBuffer.fromByteBuffer(valBuf);
    private int srSize = 0;
    private int ops = 0;

    private WeakReference<SavedResourceCollection<Album>> savedAlbums;
    private WeakReference<SavedResourceCollection<Track>> likedSongs;

    Library(SaveDirectory state)
    {
        this.state = state;
        needsFinalize = new ArrayDeque<>();
        needsSave = new ArrayDeque<>();
        needsSaveStatus = new HashMap<>();
        rq = new ReferenceQueue<>();

        env = Env.create()
                .setMapSize(state.mapSize)
                .setMaxDbs(7)
                .open(state.dbDir);
        pageSize = env.stat().pageSize;
        keyBuf = ByteBuffer.allocateDirect(env.getMaxKeySize());
        keyMemBuf = MemoryBuffer.fromByteBuffer(keyBuf);
        albumDb = new ResourceKV<>(Album.class, "album", false,
                this::writeAlbum, finalizingReaderFor(Album.class, this::albumFinalizer));
        artistDb = new ResourceKV<>(Artist.class, "artist", false,
                this::writeArtist, finalizingReaderFor(Artist.class, this::artistFinalizer));
        genreDb = new ResourceKV<>(Genre.class, "genre", true,
                fury::serialize, readerFor(Genre.class));
        labelDb = new ResourceKV<>(Label.class, "label", true,
                fury::serialize, readerFor(Label.class));
        playlistDb = new ResourceKV<>(Playlist.class, "playlist", false,
                fury::serialize, readerFor(Playlist.class));
        trackDb = new ResourceKV<>(Track.class, "track", false,
                this::writeTrack, finalizingReaderFor(Track.class, this::trackFinalizer));

        fury.registerSerializer(SavedAlbum.class, new SavedResourceSerializer<>(SavedAlbum.class, SavedAlbum::new, savedResourceFinalizer(albumDb)));
        fury.registerSerializer(SavedTrack.class, new SavedResourceSerializer<>(SavedTrack.class, SavedTrack::new, savedResourceFinalizer(trackDb)));
        savedResourceListDb = env.openDbi("savedResourceList", DbiFlags.MDB_CREATE);
    }

    /**
     * Determines if the LMDB map size "should" be increased, and if so, increases it.  The map size should be
     * substantially overestimated, as the default one is, so an invocation of this method will increase it by
     * quite a bit.  A new size is computed by multiplying the number of pages in all DBs by page size and by 4.
     * If this new size is greater than the current size, we apply the new size.
     */
    private void growMap()
    {
        final long compareSize;
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            final Stat s1 = albumDb.db.stat(txn), s2 = artistDb.db.stat(txn), s3 = genreDb.db.stat(txn),
                    s4 = labelDb.db.stat(txn), s5 = playlistDb.db.stat(txn), s6 = trackDb.db.stat(txn),
                    s7 = savedResourceListDb.stat(txn);
            compareSize = (s1.branchPages + s1.leafPages + s1.overflowPages
                    + s2.branchPages + s2.leafPages + s2.overflowPages
                    + s3.branchPages + s3.leafPages + s3.overflowPages
                    + s4.branchPages + s4.leafPages + s4.overflowPages
                    + s5.branchPages + s5.leafPages + s5.overflowPages
                    + s6.branchPages + s6.leafPages + s6.overflowPages
                    + s7.branchPages + s7.leafPages + s7.overflowPages) * pageSize * 4;
        }
        if (compareSize > state.mapSize) {
            env.setMapSize(state.mapSize = compareSize);
        }
    }

    /**
     * Invokes <code>growMap()</code> if enough operations have been performed.
     */
    private void maybeGrowMap()
    {
        if (++ops == CHECK_SIZE_FREQ) {
            growMap();
            ops = 0;
        }
    }

    private static <T extends LibraryResource, R extends SavedResource<T>> BiFunction<R, MemoryBuffer, Runnable> savedResourceFinalizer(
            final ResourceKV<T> db)
    {
        return (sr, buffer) -> {
            final String resourceKey = readResourceKeyField(buffer);
            return () -> sr.setResource(db.read(resourceKey));
        };
    }

    private static <F extends LibraryResource> F[] readResourceArray(Function<Integer, F[]> fieldArrayConstructor,
                                                                     String[] keys,
                                                                     Function<String, F> func)
    {
        if (keys != null) {
            int len = keys.length;
            F[] ret = fieldArrayConstructor.apply(len);
            for (int i = 0; i < len; i++) {
                ret[i] = func.apply(keys[i]);
            }
            return ret;
        }
        return null;
    }

    private static String[] readResourceKeyArray(MemoryBuffer buffer)
    {
        int len = buffer.readInt32();
        if (len > 0) {
            String[] ret = new String[len];
            for (int i = 0; i < len; i++) {
                ret[i] = fury.readString(buffer);
            }
            return ret;
        } else {
            return null;
        }
    }

    private static String readResourceKeyField(MemoryBuffer buffer)
    {
        String keyObj = fury.readString(buffer);
        if (keyObj.isEmpty()) {
            return null;
        } else {
            return keyObj;
        }
    }

    private static <T extends LibraryResource> void readResourceField(String key,
                                                                      Function<String, T> reader,
                                                                      Consumer<T> setter)
    {
        if (key != null) {
            T toSet = reader.apply(key);
            setter.accept(toSet);
        }
    }

    private static int roundBufSize(int n)
    {
        n |= (n - 1) >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }

    public SavedResourceCollection<Track> getLikedSongs()
    {
        SavedResourceCollection<Track> ret;
        if (likedSongs == null || (ret = likedSongs.get()) == null) {
            ret = new SavedResourceCollection<>(LIKED_SONGS_KEY);
            likedSongs = new WeakReference<>(ret);
            populateSavedResources(ret);
        }
        return ret;
    }

    public SavedResourceCollection<Album> getSavedAlbums()
    {
        SavedResourceCollection<Album> ret;
        if (savedAlbums == null || (ret = savedAlbums.get()) == null) {
            ret = new SavedResourceCollection<>(SAVED_ALBUMS_KEY);
            savedAlbums = new WeakReference<>(ret);
            populateSavedResources(ret);
        }
        return ret;
    }

    private static <T extends AbstractSpotifyResource> List<SavedResource<T>> makeSrList(List<T> resources,
                                                                                         ZonedDateTime addedAt)
    {
        return resources.stream()
                .map((Function<T, SavedResource<T>>) (t) -> SavedResource.of(addedAt, t))
                .toList();
    }

    public <T extends AbstractSpotifyResource> void saveResourcesToCollection(List<T> resources, ZonedDateTime addedAt,
                                                                              SavedResourceCollection<T> collection)
    {
        collection.addResources(makeSrList(resources, addedAt));
    }

    public <T extends AbstractSpotifyResource> void saveResourcesToCollection(List<T> resources, ZonedDateTime addedAt,
                                                                              SavedResourceCollection<T> collection,
                                                                              int i)
    {
        collection.addResourcesAt(makeSrList(resources, addedAt), i);
    }

    public <T extends AbstractSpotifyResource> void saveResourceToCollection(T resource,
                                                                             ZonedDateTime addedAt,
                                                                             SavedResourceCollection<T> collection)
    {
        if (resource != null) collection.addResource(SavedResource.of(addedAt, resource));
    }

    public <T extends AbstractSpotifyResource> void saveResourceToCollection(SavedResource<T> sr,
                                                                             SavedResourceCollection<T> collection,
                                                                             int i)
    {
        if (sr != null) collection.addResourceAt(sr, i);
    }

    public Album albumOf(String id)
    {
        return retrieveOrCreate(albumDb, id, Album::new);
    }

    public Artist artistOf(String id)
    {
        return retrieveOrCreate(artistDb, id, Artist::new);
    }

    public Genre genreOf(String name)
    {
        return retrieveOrCreate(genreDb, name, Genre::new);
    }

    public Label labelOf(String name)
    {
        return retrieveOrCreate(labelDb, name, Label::new);
    }

    public <T extends SpotifyResource> void populateSavedResources(SavedResourceCollection<T> collection)
    {
        if (collection.resources == null) {
            synchronized (env) {
                try (Txn<ByteBuffer> txn = env.txnRead()) {
                    final ByteBuffer valBuf = savedResourceListDb.get(txn, encodeKey(collection.getKey()));
                    if (valBuf == null) {
                        collection.resources = new ArrayList<>();
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<SavedResource<T>> srList = (ArrayList<SavedResource<T>>) fury.deserialize(MemoryBuffer.fromByteBuffer(valBuf));
                    collection.resources = srList;
                }
                handleFinalizationQueue();
            }
        }
    }

    private <T extends AbstractSpotifyResource> void depopulateSavedResources(SavedResourceCollection<T> collection)
    {
        ArrayList<SavedResource<T>> savedResources = collection.resources;
        if (savedResources != null) {
            synchronized (env) {
                if (srSize == 0 && !savedResources.isEmpty()) {
                    srSize = fury.serialize(savedResources.get(0)).length;
                }
                final int bufSize = savedResources.size() * srSize;
                if (valBuf.capacity() < bufSize) {
                    valBuf = ByteBuffer.allocateDirect(roundBufSize(bufSize));
                    valMemBuf = MemoryBuffer.fromByteBuffer(valBuf);
                } else {
                    valBuf.clear();
                    valMemBuf.writerIndex(0);
                }

                fury.serialize(valMemBuf, savedResources);
                ensureValOffHeap();
                savedResourceListDb.put(encodeKey(collection.getKey()), valBuf);
                growMap();
            }
        }
    }

    public Playlist playlistOf(String id)
    {
        Playlist p = retrieveOrCreate(playlistDb, id, Playlist::new);
        populateSavedResources(p);
        return p;
    }

    public Track trackOf(String id)
    {
        return retrieveOrCreate(trackDb, id, Track::new);
    }

    public List<Playlist> getPlaylists()
    {
        List<Playlist> ret = playlistDb.values();
        handleFinalizationQueue();
        return ret;
    }

    public void getPlaylists(Collection<Playlist> collection)
    {
        playlistDb.values(collection);
        handleFinalizationQueue();
    }

    public void deletePlaylist(Playlist playlist)
    {
        playlistDb.remove(playlist.getKey());
        savedResourceListDb.delete(keyBuf.rewind());
    }

    public Cleanup cleanUnusedResources()
    {
        return new Cleanup();
    }

    private <T extends LibraryResource> void markModified(final ResourceKV<T> db, final T t)
    {
        synchronized (env) {
            if (needsSaveStatus.putIfAbsent(t, SHOULD_SAVE) == null)
                needsSave.add(() -> db.save(t));
        }
    }

    public void markModified(LibraryResource lr)
    {
        if (lr instanceof Album r) markModified(r);
        else if (lr instanceof Artist r) markModified(r);
        else if (lr instanceof Genre r) markModified(r);
        else if (lr instanceof Label r) markModified(r);
        else if (lr instanceof Playlist r) markContentsModified(r);
        else if (lr instanceof SavedResourceCollection<?> r) markContentsModified(r);
        else if (lr instanceof Track r) markModified(r);
    }

    public void markModified(Album a)
    {
        markModified(albumDb, a);
    }

    public void markModified(Artist a)
    {
        markModified(artistDb, a);
    }

    public void markModified(Genre g)
    {
        markModified(genreDb, g);
    }

    public void markModified(Label l)
    {
        markModified(labelDb, l);
    }

    public void markModified(Playlist p)
    {
        synchronized (env) {
            Byte curr = needsSaveStatus.getOrDefault(p, (byte) 0);
            if ((curr & SHOULD_SAVE) == 0) {
                needsSave.add(() -> playlistDb.save(p));
                needsSaveStatus.put(p, (byte) (curr | SHOULD_SAVE));
            }
        }
    }

    public void markContentsModified(Playlist p)
    {
        synchronized (env) {
            Byte curr = needsSaveStatus.getOrDefault(p, (byte) 0);
            if ((curr & SHOULD_SAVE_CONTENTS) == 0) {
                needsSave.add(() -> depopulateSavedResources(p));
                needsSaveStatus.put(p, (byte) (curr | SHOULD_SAVE_CONTENTS));
            }
        }
    }

    public void markContentsModified(SavedResourceCollection<?> src)
    {
        synchronized (env) {
            if (needsSaveStatus.putIfAbsent(src, SHOULD_SAVE_CONTENTS) == null)
                needsSave.add(() -> depopulateSavedResources(src));
        }
    }

    public void markModified(Track t)
    {
        markModified(trackDb, t);
    }

    public boolean hasModified()
    {
        synchronized (env) {
            return !needsSave.isEmpty();
        }
    }

    public void saveModified()
    {
        synchronized (env) {
            Runnable r;
            while ((r = needsSave.poll()) != null)
                r.run();
            growMap();
        }
    }

    @Override
    public void close()
    throws IOException
    {
        synchronized (env) {
            albumDb.close();
            artistDb.close();
            genreDb.close();
            labelDb.close();
            playlistDb.close();
            trackDb.close();
            env.close();
            state.saveData();
        }
    }

    private void writeResourceField(MemoryBuffer buffer, LibraryResource field)
    {
        fury.writeString(buffer, field == null ? "" : field.getKey());
    }

    private void writeResourceArray(MemoryBuffer buffer, LibraryResource[] resources)
    {
        if (resources == null) {
            buffer.writeInt32(0);
        } else {
            int len = resources.length;
            buffer.writeInt32(len);
            if (len > 0) {
                for (LibraryResource resource : resources) {
                    fury.writeString(buffer, resource.getKey());
                }
            }
        }
    }

    private void handleFinalizationQueue()
    {
        Runnable f;
        while ((f = needsFinalize.poll()) != null)
            f.run();
    }

    private void writeAlbum(MemoryBuffer buffer, Album album)
    {
        fury.serialize(buffer, album);
        writeResourceField(buffer, album.getLabel());
        writeResourceArray(buffer, album.getArtists());
        writeResourceArray(buffer, album.getTracks());
        writeResourceArray(buffer, album.getGenres());
    }

    private static <T> Function<MemoryBuffer, T> readerFor(Class<T> valueClass)
    {
        return buffer -> valueClass.cast(fury.deserialize(buffer));
    }

    private Runnable albumFinalizer(final Album album, MemoryBuffer buffer)
    {
        final String labelKey = readResourceKeyField(buffer);
        final String[] artistKeys = readResourceKeyArray(buffer);
        final String[] trackKeys = readResourceKeyArray(buffer);
        final String[] genreKeys = readResourceKeyArray(buffer);
        return () -> {
            readResourceField(labelKey, labelDb::read, album::setLabel);
            album.setArtists(readResourceArray(Artist[]::new, artistKeys, artistDb::read));
            album.setTracks(readResourceArray(Track[]::new, trackKeys, trackDb::read));
            album.setGenres(readResourceArray(Genre[]::new, genreKeys, genreDb::read));
        };
    }

    private Runnable artistFinalizer(final Artist artist, MemoryBuffer buffer)
    {
        final String[] genreKeys = readResourceKeyArray(buffer);
        return () -> artist.setGenres(readResourceArray(Genre[]::new, genreKeys, genreDb::read));
    }

    private Runnable trackFinalizer(final Track track, MemoryBuffer buffer)
    {
        final String albumKey = readResourceKeyField(buffer);
        final String[] artistKeys = readResourceKeyArray(buffer);
        return () -> {
            readResourceField(albumKey, albumDb::read, track::setAlbum);
            track.setArtists(readResourceArray(Artist[]::new, artistKeys, artistDb::read));
        };
    }

    private <T extends LibraryResource> Function<MemoryBuffer, T> finalizingReaderFor(Class<T> valueClass,
                                                                                      BiFunction<T, MemoryBuffer, Runnable> finalizer)
    {
        return buffer -> {
            T ret = valueClass.cast(fury.deserialize(buffer));
            needsFinalize.add(finalizer.apply(ret, buffer));
            return ret;
        };
    }

    private void writeArtist(MemoryBuffer buffer, Artist artist)
    {
        fury.serialize(buffer, artist);
        writeResourceArray(buffer, artist.getGenres());
    }

    private void writeTrack(MemoryBuffer buffer, Track track)
    {
        fury.serialize(buffer, track);
        writeResourceField(buffer, track.getAlbum());
        writeResourceArray(buffer, track.getArtists());
    }

    private <T extends LibraryResource> T retrieveOrCreate(ResourceKV<T> db, String key, Function<String, T> func)
    {
        T ret = db.readOrCreate(key, func);
        handleFinalizationQueue();
        return ret;
    }

    private void ensureValOffHeap()
    {
        if (valMemBuf.isOffHeap()) {
            valBuf.limit(valMemBuf.writerIndex());
        } else {
            byte[] onHeap = valMemBuf.getArray();
            System.err.println("WARNING: USING HEAP MEMORY for buffer size " + onHeap.length);
            valBuf = ByteBuffer.allocateDirect(roundBufSize(onHeap.length)).put(onHeap).flip();
            valMemBuf = MemoryBuffer.fromByteBuffer(valBuf);
        }
    }

    private String decodeKey(ByteBuffer keyBuf)
    {
        return fury.readString(MemoryBuffer.fromByteBuffer(keyBuf));
    }

    private ByteBuffer encodeKey(String key)
    {
        keyMemBuf.writerIndex(0);
        fury.writeString(keyMemBuf, key);
        return keyBuf.limit(keyMemBuf.writerIndex()).rewind();
    }

    private void evictResourceCacheNodes()
    {
        synchronized (rq) {
            Object r;
            while ((r = rq.poll()) != null) {
                ((ResourceCacheNode) r).evict();
            }
        }
    }

    private class SavedResourceSerializer<T extends SavedResource<?>>
            extends Serializer<T>
    {
        private final Supplier<T> constructor;
        private final BiFunction<T, MemoryBuffer, Runnable> finalizer;

        private SavedResourceSerializer(Class<T> type,
                                        Supplier<T> constructor,
                                        BiFunction<T, MemoryBuffer, Runnable> finalizer)
        {
            super(Library.fury, type);
            this.constructor = constructor;
            this.finalizer = finalizer;
        }

        @Override
        public T read(MemoryBuffer buffer)
        {
            T t = constructor.get();
            t.setAddedAt((ZonedDateTime) fury.readNonRef(buffer));
            needsFinalize.add(finalizer.apply(t, buffer));
            return t;
        }

        @Override
        public void write(MemoryBuffer buffer, T value)
        {
            fury.writeNonRef(buffer, value.addedAt());
            fury.writeString(buffer, value.getResource().getKey());
        }
    }

    private class ResourceKV<T extends LibraryResource>
            implements AutoCloseable
    {
        private final Class<T> valueClass;
        private final boolean shouldCommitOnInstantiation;
        private final Dbi<ByteBuffer> db;
        private final Map<String, ResourceCacheNode> cache;
        private final BiConsumer<MemoryBuffer, T> serializer;
        private final Function<MemoryBuffer, T> deserializer;

        private ResourceKV(Class<T> valueClass,
                           String dbKey,
                           boolean shouldCommitOnInstantiation,
                           BiConsumer<MemoryBuffer, T> serializer,
                           Function<MemoryBuffer, T> deserializer)
        {
            this.valueClass = valueClass;
            this.shouldCommitOnInstantiation = shouldCommitOnInstantiation;
            synchronized (env) {
                this.db = env.openDbi(dbKey, DbiFlags.MDB_CREATE);
            }
            cache = new HashMap<>();

            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        private void save(T obj)
        {
            synchronized (env) {
                put(encodeKey(obj.getKey()), obj);
            }
        }

        private T read(String key)
        {
            synchronized (env) {
                LibraryResource res = tryFromCache(key);
                T ret;
                if (res == null) {
                    final ByteBuffer keyBuf = encodeKey(key);
                    ret = tryFromDB(keyBuf);
                    if (ret != null) putInCache(ret);
                } else {
                    ret = valueClass.cast(res);
                    putInCache(ret);
                }
                return ret;
            }
        }

        private T readOrCreate(String key, Function<String, T> func)
        {
            synchronized (env) {
                evictResourceCacheNodes();
                LibraryResource res = tryFromCache(key);
                T obj;
                if (res == null) {
                    final ByteBuffer keyBuf = encodeKey(key);
                    res = tryFromDB(keyBuf);
                    if (res == null) {
                        obj = func.apply(key);
                        if (shouldCommitOnInstantiation) put(keyBuf, obj);
                    } else {
                        obj = valueClass.cast(res);
                    }
                    putInCache(obj);
                } else {
                    obj = valueClass.cast(res);
                }
                return obj;
            }
        }

        private void values(Collection<T> collection)
        {
            LibraryResource r;
            T obj;
            ByteBuffer valBuf;
            Set<String> keysFound = new HashSet<>();
            synchronized (env) {
                evictResourceCacheNodes();

                synchronized (rq) {
                    for (ResourceCacheNode cacheNode : cache.values()) {
                        r = cacheNode.get();
                        if (r != null) {
                            keysFound.add(r.getKey());
                            collection.add(valueClass.cast(r));
                        }
                    }
                }

                try (Txn<ByteBuffer> txn = env.txnRead();
                     CursorIterable<ByteBuffer> it = db.iterate(txn)) {
                    for (CursorIterable.KeyVal<ByteBuffer> c : it) {
                        if (!keysFound.contains(decodeKey(c.key()))) {
                            valBuf = c.val();
                            if (valBuf != null) {
                                obj = deserializer.apply(MemoryBuffer.fromByteBuffer(valBuf));
                                collection.add(obj);
                                putInCache(obj);
                            }
                        }
                    }
                }
            }
        }

        private List<T> values()
        {
            List<T> list = new ArrayList<>();
            values(list);
            return list;
        }

        private void remove(String key)
        {
            synchronized (env) {
                cache.remove(key);
                db.delete(encodeKey(key));
            }
        }

        @Override
        public void close()
        {
            synchronized (env) {
                db.close();
            }
        }

        private void putInCache(T obj)
        {
            String objKey = obj.getKey();
            synchronized (rq) {
                cache.put(objKey, new ResourceCacheNode(objKey, obj, rq, cache::remove));
            }
        }

        private LibraryResource tryFromCache(String key)
        {
            synchronized (rq) {
                ResourceCacheNode node = cache.get(key);
                if (node != null)
                    return node.get();
                return null;
            }
        }

        private T tryFromDB(Txn<ByteBuffer> txn, ByteBuffer keyBuf)
        {
            final ByteBuffer valBuf = db.get(txn, keyBuf);
            if (valBuf == null) return null;
            return deserializer.apply(MemoryBuffer.fromByteBuffer(valBuf));
        }

        private T tryFromDB(ByteBuffer keyBuf)
        {
            try (Txn<ByteBuffer> txn = env.txnRead()) {
                return tryFromDB(txn, keyBuf);
            }
        }

        private void put(ByteBuffer keyBuf, T val)
        {
            valBuf.clear();
            valMemBuf.writerIndex(0);
            serializer.accept(valMemBuf, val);
            ensureValOffHeap();
            db.put(keyBuf, valBuf);
            maybeGrowMap();
        }
    }

    private static class ResourceCacheNode
            extends WeakReference<LibraryResource>
    {
        private final String key;
        private final Consumer<String> onEvict;

        private ResourceCacheNode(String key, LibraryResource val, ReferenceQueue<LibraryResource> rq,
                                  Consumer<String> onEvict)
        {
            super(val, rq);
            this.key = key;
            this.onEvict = onEvict;
        }

        private void evict()
        {
            onEvict.accept(key);
        }
    }

    public final class Cleanup
    {
        private final Set<Album> albumsToRemove;
        private final Set<Artist> artistsToRemove;
        private final Set<Genre> genresToRemove;
        private final Set<Label> labelsToRemove;
        private final Set<Track> tracksToRemove;
        private final List<LibraryResource> recovered;
        private boolean recoverable = false;

        private Cleanup()
        {
            albumDb.values(albumsToRemove = new HashSet<>());
            artistDb.values(artistsToRemove = new HashSet<>());
            genreDb.values(genresToRemove = new HashSet<>());
            labelDb.values(labelsToRemove = new HashSet<>());
            trackDb.values(tracksToRemove = new HashSet<>());
            List<Playlist> playlists = playlistDb.values();
            handleFinalizationQueue();

            SavedResourceCollection<Album> savedAlbums = getSavedAlbums();
            for (SavedResource<Album> s : savedAlbums.getSavedResources()) {
                keepAlbum(s.getResource());
            }

            for (Playlist p : playlists) {
                populateSavedResources(p);
                for (SavedResource<Track> s : p.getSavedResources()) {
                    keepTrack(s.getResource());
                }
            }

            SavedResourceCollection<Track> likedSongs = getLikedSongs();
            for (SavedResource<Track> s : likedSongs.getSavedResources()) {
                keepTrack(s.getResource());
            }

            recovered = new ArrayList<>();
            recoverable = true;
        }

        public void forEachResource(Consumer<LibraryResource> func)
        {
            tracksToRemove.forEach(func);
            albumsToRemove.forEach(func);
            artistsToRemove.forEach(func);
            genresToRemove.forEach(func);
            labelsToRemove.forEach(func);
        }

        public synchronized void clean()
        {
            for (Album a : albumsToRemove) albumDb.remove(a.getId());
            for (Artist a : artistsToRemove) artistDb.remove(a.getId());
            for (Genre g : genresToRemove) genreDb.remove(g.getName());
            for (Label l : labelsToRemove) labelDb.remove(l.getName());
            for (Track t : tracksToRemove) trackDb.remove(t.getId());
        }

        private void recover(LibraryResource resource)
        {
            if (recoverable) recovered.add(resource);
        }

        public synchronized List<LibraryResource> keep(LibraryResource lr)
        {
            final int left = recovered.size();

            if (lr instanceof Album) keepAlbum((Album) lr);
            else if (lr instanceof Artist) keepArtist((Artist) lr);
            else if (lr instanceof Genre) keepGenre((Genre) lr);
            else if (lr instanceof Label) keepLabel((Label) lr);
            else if (lr instanceof Track) keepTrack((Track) lr);

            return Collections.unmodifiableList(recovered.subList(left, recovered.size()));
        }

        private void keepAlbum(Album a)
        {
            if (albumsToRemove.remove(a)) {
                recover(a);
                Track[] t = a.getTracks();
                if (t != null) for (Track tr : t)
                    keepTrackOnly(tr);
                Artist[] myArtists = a.getArtists();
                if (myArtists != null) for (Artist ar : myArtists)
                    keepArtist(ar);
                Label l = a.getLabel();
                if (l != null)
                    keepLabel(a.getLabel());
                Genre[] myGenres = a.getGenres();
                if (myGenres != null) for (Genre g : myGenres)
                    keepGenre(g);
            }
        }

        private void keepArtist(Artist a)
        {
            if (artistsToRemove.remove(a)) {
                recover(a);
                Genre[] g = a.getGenres();
                if (g != null) for (Genre ge : g)
                    keepGenre(ge);
            }
        }

        private boolean keepTrackOnly(Track t)
        {
            if (tracksToRemove.remove(t)) {
                recover(t);
                Artist[] myArtists = t.getArtists();
                if (myArtists != null) for (Artist a : myArtists)
                    keepArtist(a);
                return true;
            }
            return false;
        }

        private void keepTrack(Track t)
        {
            Album a = t.getAlbum();
            if (keepTrackOnly(t) && a != null)
                keepAlbum(a);
        }

        private void keepGenre(Genre g)
        {
            recover(g);
            genresToRemove.remove(g);
        }

        private void keepLabel(Label l)
        {
            recover(l);
            labelsToRemove.remove(l);
        }
    }
}
