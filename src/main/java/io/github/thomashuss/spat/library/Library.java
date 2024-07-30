package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.Spat;
import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
    public static final String LIKED_SONGS_KEY = "likedSongs";
    public static final String SAVED_ALBUMS_KEY = "savedAlbums";

    // No need for a thread safe instance due to synchronization on lmdb env.
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

    private final Dbi<ByteBuffer> savedResourceListDb;
    private final Env<ByteBuffer> env;
    private final Queue<Runnable> needsFinalize;
    private final ResourceKV<Album> albumDb;
    private final ResourceKV<Artist> artistDb;
    private final ResourceKV<Genre> genreDb;
    private final ResourceKV<Label> labelDb;
    private final ResourceKV<Playlist> playlistDb;
    private final ResourceKV<Track> trackDb;
    private final Set<LibraryResource> needsSave;
    private final ByteBuffer keyBuf;
    private final MemoryBuffer keyMemBuf;
    private ByteBuffer valBuf = ByteBuffer.allocateDirect(1024);
    private MemoryBuffer valMemBuf = MemoryBuffer.fromByteBuffer(valBuf);
    private int srSize = 0;

    private WeakReference<SavedResourceCollection<Album>> savedAlbums;
    private WeakReference<SavedResourceCollection<Track>> likedSongs;

    Library(File dbPath)
    {
        needsFinalize = new ArrayDeque<>();
        needsSave = new HashSet<>();

        env = Env.create()
                .setMapSize(10_485_760)  // TODO: handle map growth
                .setMaxDbs(7)
                .open(dbPath);
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
        savedResourceListDb = env.openDbi("savedResourceList", DbiFlags.MDB_CREATE);
        trackDb = new ResourceKV<>(Track.class, "track", false,
                this::writeTrack, finalizingReaderFor(Track.class, this::trackFinalizer));

        fury.registerSerializer(SavedAlbum.class, new SavedResourceSerializer<>(SavedAlbum.class, SavedAlbum::new, savedResourceFinalizer(albumDb)));
        fury.registerSerializer(SavedTrack.class, new SavedResourceSerializer<>(SavedTrack.class, SavedTrack::new, savedResourceFinalizer(trackDb)));
    }

    private static <T extends LibraryResource, R extends SavedResource<T>> BiFunction<R, MemoryBuffer, Runnable> savedResourceFinalizer(
            final ResourceKV<T> db)
    {
        return (sr, buffer) -> {
            final String resourceKey = readResourceKeyField(buffer);
            return () -> sr.setResource(db.read(resourceKey));
        };
    }

    private static <F extends LibraryResource> F[] readResourceArray(Class<F> fieldClass,
                                                                     String[] keys,
                                                                     Function<String, F> func)
    {
        if (keys != null) {
            int len = keys.length;
            @SuppressWarnings("unchecked")
            F[] ret = (F[]) Array.newInstance(fieldClass, len);
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

    public void saveAlbum(ZonedDateTime addedAt, Album album)
    {
        SavedResourceCollection<Album> savedAlbums = getSavedAlbums();
        if (album != null) savedAlbums.addResource(new SavedAlbum(addedAt, album));
    }

    public void saveTrackToCollection(Track track, ZonedDateTime addedAt, SavedResourceCollection<Track> collection)
    {
        if (track != null) collection.addResource(new SavedTrack(addedAt, track));
    }

    public void unsaveAlbum(Album album)
    {
        SavedResourceCollection<Album> savedAlbums = getSavedAlbums();
        if (album != null) savedAlbums.removeResource(album);
    }

    public void unsaveAlbum(int i)
    {
        SavedResourceCollection<Album> savedAlbums = getSavedAlbums();
        savedAlbums.removeResource(i);
    }

    public void clearSavedAlbums()
    {
        SavedResourceCollection<Album> savedAlbums = getSavedAlbums();
        savedAlbums.clearResources();
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

    private <T extends SpotifyResource> void depopulateSavedResources(SavedResourceCollection<T> collection)
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

    public Cleanup cleanUnusedResources()
    {
        return new Cleanup();
    }

    public void markModified(LibraryResource resource)
    {
        needsSave.add(resource);
    }

    public boolean hasModified()
    {
        return !needsSave.isEmpty();
    }

    public void saveModified()
    {
        Iterator<LibraryResource> it = needsSave.iterator();
        LibraryResource lr;
        while (it.hasNext()) {
            lr = it.next();
            if (lr instanceof Album) albumDb.save((Album) lr);
            else if (lr instanceof Artist) artistDb.save((Artist) lr);
            else if (lr instanceof Genre) genreDb.save((Genre) lr);
            else if (lr instanceof Label) labelDb.save((Label) lr);
            else if (lr instanceof Playlist p) {
                playlistDb.save(p);
                depopulateSavedResources(p);
            } else if (lr instanceof SavedResourceCollection<?> src) depopulateSavedResources(src);
            else if (lr instanceof Track) trackDb.save((Track) lr);
            it.remove();
        }
    }

    @Override
    public void close()
    {
        albumDb.close();
        artistDb.close();
        genreDb.close();
        labelDb.close();
        playlistDb.close();
        trackDb.close();
        env.close();
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

    private <T> Function<MemoryBuffer, T> readerFor(Class<T> valueClass)
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
            album.setArtists(readResourceArray(Artist.class, artistKeys, artistDb::read));
            album.setTracks(readResourceArray(Track.class, trackKeys, trackDb::read));
            album.setGenres(readResourceArray(Genre.class, genreKeys, genreDb::read));
        };
    }

    private Runnable artistFinalizer(final Artist artist, MemoryBuffer buffer)
    {
        final String[] genreKeys = readResourceKeyArray(buffer);
        return () -> artist.setGenres(readResourceArray(Genre.class, genreKeys, genreDb::read));
    }

    private Runnable trackFinalizer(final Track track, MemoryBuffer buffer)
    {
        final String albumKey = readResourceKeyField(buffer);
        final String[] artistKeys = readResourceKeyArray(buffer);
        return () -> {
            readResourceField(albumKey, albumDb::read, track::setAlbum);
            track.setArtists(readResourceArray(Artist.class, artistKeys, artistDb::read));
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

    private class ResourceKV<T extends LibraryResource>  // TODO: may not need to be nested
            implements AutoCloseable
    {
        private final Class<T> valueClass;
        private final boolean shouldCommitOnInstantiation;
        private final Dbi<ByteBuffer> db;
        private final Map<String, ResourceCacheNode> cache;
        private final ReferenceQueue<LibraryResource> rq;
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
            rq = new ReferenceQueue<>();
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
                expungeStaleEntries();
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
                expungeStaleEntries();

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
                cache.put(objKey, new ResourceCacheNode(objKey, obj, rq));
            }
        }

        private void expungeStaleEntries()
        {
            synchronized (rq) {
                Object r;
                while ((r = rq.poll()) != null) {
                    cache.remove(((ResourceCacheNode) r).key);
                }
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
        }

        private static class ResourceCacheNode
                extends WeakReference<LibraryResource>
        {
            private final String key;

            private ResourceCacheNode(String key, LibraryResource val, ReferenceQueue<LibraryResource> rq)
            {
                super(val, rq);
                this.key = key;
            }
        }
    }

    public final class Cleanup
    {
        private final Set<Album> albumsToRemove;
        private final Set<Artist> artistsToRemove;
        private final Set<Genre> genresToRemove;
        private final Set<Label> labelsToRemove;
        private final Set<Track> tracksToRemove;
        private boolean recoverable = false;
        private Recovered recovered;
        private Recovered recoveredTail;

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

            recoverable = true;
        }

        public Stream<? extends LibraryResource> getResourceStream()
        {
            return Stream.of(tracksToRemove.stream(), albumsToRemove.stream(),
                    artistsToRemove.stream(), genresToRemove.stream(), labelsToRemove.stream()).flatMap(s -> s);
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
            Recovered thisRecovered = new Recovered(resource);
            if (recoveredTail == null) recovered = recoveredTail = thisRecovered;
            else {
                recoveredTail.next = thisRecovered;
                recoveredTail = thisRecovered;
            }
        }

        public synchronized Recovered keep(LibraryResource lr)
        {
            Recovered head = recoveredTail;

            if (lr instanceof Album) keepAlbum((Album) lr);
            else if (lr instanceof Artist) keepArtist((Artist) lr);
            else if (lr instanceof Genre) keepGenre((Genre) lr);
            else if (lr instanceof Label) keepLabel((Label) lr);
            else if (lr instanceof Track) keepTrack((Track) lr);

            return head == null ? recovered : head.next;
        }

        private void keepAlbum(Album a)
        {
            if (albumsToRemove.remove(a)) {
                if (recoverable) recover(a);
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
                if (recoverable) recover(a);
                Genre[] g = a.getGenres();
                if (g != null) for (Genre ge : g)
                    keepGenre(ge);
            }
        }

        private boolean keepTrackOnly(Track t)
        {
            if (tracksToRemove.remove(t)) {
                if (recoverable) recover(t);
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
            if (recoverable) recover(g);
            genresToRemove.remove(g);
        }

        private void keepLabel(Label l)
        {
            if (recoverable) recover(l);
            labelsToRemove.remove(l);
        }

        public static final class Recovered
        {
            public final LibraryResource resource;
            private Recovered next;

            private Recovered(LibraryResource resource)
            {
                this.resource = resource;
            }

            public Recovered next()
            {
                return next;
            }
        }
    }
}
