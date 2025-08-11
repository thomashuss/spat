package io.github.thomashuss.jspat.library;

import io.github.thomashuss.jspat.JSpat;
import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coherently tracks all library resources.
 */
/*
 * Playlist -> Saved -> Track -----┐
 *               |        🡙        ￬
 *               └----> Album -> Artist
 *                        |        ￬
 *                        └----> Genre
 *                        └----> Label
 */
public final class Library
        implements AutoCloseable
{
    public static final long INITIAL_MAP_SIZE = 100_485_760;
    /**
     * No need for a thread safe instance due to synchronization on lmdb env.
     */
    static final Fury fury = JSpat.fury;
    private static final String LIKED_SONGS_KEY = "likedSongs";
    private static final String SAVED_ALBUMS_KEY = "savedAlbums";
    /**
     * Purely heuristic.
     */
    private static final int CHECK_SIZE_FREQ = 512;
    private static final byte NO_SAVE = 0;
    private static final byte SHOULD_SAVE = 1;
    private static final byte SHOULD_SAVE_CONTENTS = 2;

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

    final Env<ByteBuffer> env;
    final Queue<Runnable> needsFinalize;
    final ReferenceQueue<LibraryResource> rq;
    private final SaveDirectory state;
    private final int pageSize;
    private final Dbi<ByteBuffer> savedResourceListDb;
    final ResourceKV<Album> albumDb;
    final ResourceKV<Artist> artistDb;
    final ResourceKV<Genre> genreDb;
    final ResourceKV<Label> labelDb;
    private final ResourceKV<Playlist> playlistDb;
    final ResourceKV<Track> trackDb;
    private final Map<LibraryResource, Byte> needsSaveStatus;
    private final Queue<Runnable> needsSave;
    private final ByteBuffer keyBuf;
    private final MemoryBuffer keyMemBuf;
    ByteBuffer valBuf = ByteBuffer.allocateDirect(1024);
    MemoryBuffer valMemBuf = MemoryBuffer.fromByteBuffer(valBuf);
    private int srSize = 0;
    private int ops = 0;

    private WeakReference<SavedAlbumCollection> savedAlbums;
    private WeakReference<SavedTrackCollection> likedSongs;

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
        albumDb = new AlbumKV(this);
        artistDb = new ArtistKV(this);
        genreDb = new GenreKV(this);
        labelDb = new LabelKV(this);
        playlistDb = new PlaylistKV(this);
        trackDb = new TrackKV(this);

        fury.registerSerializer(SavedAlbum.class, new SavedAlbumSerializer(this));
        fury.registerSerializer(SavedTrack.class, new SavedTrackSerializer(this));
        savedResourceListDb = env.openDbi("savedResourceList", DbiFlags.MDB_CREATE);
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

    private static Byte getUnmodifiedContentsMark(LibraryResource ignored, byte curr)
    {
        curr &= ~SHOULD_SAVE_CONTENTS;
        if (curr == 0) return null;
        return curr;
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
    void maybeGrowMap()
    {
        if (++ops == CHECK_SIZE_FREQ) {
            growMap();
            ops = 0;
        }
    }

    private <T extends SavedResourceCollection<R>, R extends SpotifyResource> T
    getSavedResourceCollection(WeakReference<T> ref, Supplier<T> constructor, Consumer<WeakReference<T>> setter)
    {
        T ret;
        if (ref == null || (ret = ref.get()) == null) {
            ret = constructor.get();
            setter.accept(new WeakReference<>(ret));
            populateSavedResources(ret);
        }
        return ret;
    }

    public SavedTrackCollection getLikedSongs()
    {
        return getSavedResourceCollection(likedSongs, () -> new SavedTrackCollection(LIKED_SONGS_KEY),
                (ref) -> likedSongs = ref);
    }

    public SavedAlbumCollection getSavedAlbums()
    {
        return getSavedResourceCollection(savedAlbums, () -> new SavedAlbumCollection(SAVED_ALBUMS_KEY),
                (ref) -> savedAlbums = ref);
    }

    public Album getOrCreateAlbum(String id)
    {
        return retrieveOrCreate(albumDb, id, Album::new);
    }

    public Artist getOrCreateArtist(String id)
    {
        return retrieveOrCreate(artistDb, id, Artist::new);
    }

    public Genre getOrCreateGenre(String name)
    {
        return retrieveOrCreate(genreDb, name, Genre::new);
    }

    public Label getOrCreateLabel(String name)
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

    public Playlist getOrCreatePlaylist(String id)
    {
        Playlist p = retrieveOrCreate(playlistDb, id, Playlist::new);
        populateSavedResources(p);
        return p;
    }

    public Track getOrCreateTrack(String id)
    {
        return retrieveOrCreate(trackDb, id, Track::new);
    }

    public Track getTrack(String id)
    {
        return retrieveOrCreate(trackDb, id, null);
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

    private <T extends LibraryResource> void doSave(final T resource, final Consumer<T> saveFunc, final byte action)
    {
        byte curr = needsSaveStatus.getOrDefault(resource, NO_SAVE);
        if ((curr & action) == action) {
            saveFunc.accept(resource);
            curr &= (byte) ~action;
            if (curr == 0) needsSaveStatus.remove(resource);
            else needsSaveStatus.put(resource, curr);
        }
    }

    private <T extends LibraryResource> Runnable getDoSave(final T resource, final Consumer<T> saveFunc)
    {
        return () -> doSave(resource, saveFunc, SHOULD_SAVE);
    }

    private <T extends LibraryResource> Runnable getDoSaveContents(final T resource, final Consumer<T> saveFunc)
    {
        return () -> doSave(resource, saveFunc, SHOULD_SAVE_CONTENTS);
    }

    private <T extends LibraryResource> void markModified(final ResourceKV<T> db, final T t)
    {
        synchronized (env) {
            if (needsSaveStatus.putIfAbsent(t, SHOULD_SAVE) == null) {
                needsSave.add(getDoSave(t, db::save));
            }
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
            byte curr = needsSaveStatus.getOrDefault(p, NO_SAVE);
            if ((curr & SHOULD_SAVE) == 0) {
                needsSave.add(getDoSave(p, playlistDb::save));
                needsSaveStatus.put(p, (byte) (curr | SHOULD_SAVE));
            }
        }
    }

    public void markContentsModified(Playlist p)
    {
        synchronized (env) {
            byte curr = needsSaveStatus.getOrDefault(p, NO_SAVE);
            if ((curr & SHOULD_SAVE_CONTENTS) == 0) {
                needsSave.add(getDoSaveContents(p, this::depopulateSavedResources));
                needsSaveStatus.put(p, (byte) (curr | SHOULD_SAVE_CONTENTS));
            }
        }
    }

    public void markContentsModified(SavedResourceCollection<?> src)
    {
        if (src instanceof Playlist p) markContentsModified(p);
        else synchronized (env) {
            if (needsSaveStatus.putIfAbsent(src, SHOULD_SAVE_CONTENTS) == null) {
                needsSave.add(getDoSaveContents(src, this::depopulateSavedResources));
            }
        }
    }

    /*private static Byte getUnmodifiedMark(LibraryResource ignored, byte curr)
    {
        curr &= ~SHOULD_SAVE;
        if (curr == 0) return null;
        return curr;
    }

    public void unmarkModified(LibraryResource lr)
    {
        synchronized (env) {
            needsSaveStatus.computeIfPresent(lr, Library::getUnmodifiedMark);
        }
    }*/

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

    public void unmarkContentsModified(SavedResourceCollection<?> src)
    {
        synchronized (env) {
            needsSaveStatus.computeIfPresent(src, Library::getUnmodifiedContentsMark);
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

    private void handleFinalizationQueue()
    {
        Runnable f;
        while ((f = needsFinalize.poll()) != null)
            f.run();
    }

    private <T extends LibraryResource> T retrieveOrCreate(ResourceKV<T> db, String key, Function<String, T> func)
    {
        T ret = db.readOrCreate(key, func);
        handleFinalizationQueue();
        return ret;
    }

    void ensureValOffHeap()
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

    String decodeKey(ByteBuffer keyBuf)
    {
        return fury.readString(MemoryBuffer.fromByteBuffer(keyBuf));
    }

    ByteBuffer encodeKey(String key)
    {
        keyMemBuf.writerIndex(0);
        fury.writeString(keyMemBuf, key);
        return keyBuf.limit(keyMemBuf.writerIndex()).rewind();
    }

    void evictResourceCacheNodes()
    {
        synchronized (rq) {
            Object r;
            while ((r = rq.poll()) != null) {
                ((ResourceCacheNode) r).evict();
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

            SavedAlbumCollection savedAlbums = getSavedAlbums();
            for (SavedResource<Album> s : savedAlbums.getSavedResources()) {
                keepAlbum(s.getResource());
            }

            for (Playlist p : playlists) {
                populateSavedResources(p);
                for (SavedResource<Track> s : p.getSavedResources()) {
                    keepTrack(s.getResource());
                }
            }

            SavedTrackCollection likedSongs = getLikedSongs();
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
