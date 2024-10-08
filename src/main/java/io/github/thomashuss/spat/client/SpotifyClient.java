package io.github.thomashuss.spat.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.Album;
import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.AudioFeatures;
import io.github.thomashuss.spat.library.Genre;
import io.github.thomashuss.spat.library.Label;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A generic Spotify client.  The client does not update a resource unless the resource
 * is new to the library or an <code>update</code> method is invoked.
 */
public class SpotifyClient
        extends SpotifyHttpClient
{
    private static final int MAXIMUM_ARTIST_IDS_REQUEST = 50;
    private static final int MAXIMUM_SAVED_RESOURCES_REQUEST = 50;
    private static final int MAXIMUM_TRACK_IDS_REQUEST = 100;
    private static final int MAXIMUM_ALBUM_IDS_REQUEST = 20;
    private static final URL SAVED_TRACKS_URL;

    static {
        try {
            SAVED_TRACKS_URL = new URI("https://api.spotify.com/v1/me/tracks").toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ObjectMapper mapper = Spat.mapper;
    private Library library;

    @Override
    SpotifyToken parseToken(BufferedReader reader)
    throws IOException
    {
        return mapper.readValue(reader, SpotifyToken.class);
    }

    public void setLibrary(Library library)
    {
        this.library = library;
    }

    public synchronized void populatePlaylist(Playlist p,
                                              ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        String apiUrl;
        JsonNode root;
        JsonNode items;
        int size = 0;
        float progress = 0;
        progressTracker.updateProgress(0);

        apiUrl = "https://api.spotify.com/v1/playlists/" + p.getId();
        root = apiToTree(makeUri(apiUrl));
        p.setSnapshotId(root.get("snapshot_id").asText());

        root = root.get("tracks");
        if (root != null) {
            p.clearResources();
            do {
                if (size == 0) {
                    if ((size = root.get("total").asInt(0)) == 0) {
                        break;
                    }
                }
                if ((items = root.get("items")) != null) {
                    treeToSavedTrackCollection(items, p);
                    progress += (float) items.size() / size * 100;
                    progressTracker.updateProgress((int) progress);
                }
            } while ((root = root.get("next")) != null
                    && (apiUrl = root.asText(null)) != null
                    && (root = apiToTree(makeUri(apiUrl))) != null);
            library.markContentsModified(p);
        }
        progressTracker.updateProgress(100);
    }

    /**
     * Does not move track in playlist object
     */
    public synchronized void reorderPlaylist(Playlist playlist, int insertBefore,
                                             int rangeStart, int rangeLength)
    throws IOException, SpotifyClientException
    {
        try (BufferedReader reader = getAPIReader(makeUri("https://api.spotify.com/v1/playlists/"
                        + playlist.getId() + "/tracks"), "PUT",
                "{\"range_start\":" + rangeStart + ",\"insert_before\":"
                        + insertBefore + ",\"range_length\":" + rangeLength
                        + ",\"snapshot_id\":\"" + playlist.getSnapshotId() + "\"}")) {
            playlist.setSnapshotId(mapper.readTree(reader).get("snapshot_id").asText());
        }
    }

    private void addTracksFromSubList(URL url, Playlist playlist, List<Track> tracks, int atPosition)
    throws IOException, SpotifyClientException
    {
        StringBuilder body = new StringBuilder("{\"uris\":[\"spotify:track:");
        Iterator<Track> it = tracks.iterator();
        while (true) {
            body.append(it.next().getId());
            if (it.hasNext()) body.append("\",\"spotify:track:");
            else break;
        }
        body.append("\"]");
        if (atPosition >= 0) {
            body.append(",\"position\":");
            body.append(atPosition);
        }
        body.append('}');

        try (BufferedReader reader = getAPIReader(url, body.toString())) {
            playlist.setSnapshotId(mapper.readTree(reader).get("snapshot_id").asText());
        }
    }

    /**
     * Does not add track in playlist object
     */
    public synchronized void addTracksToPlaylist(final Playlist playlist,
                                                 final List<Track> tracks, final int atPosition,
                                                 final ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        if (!tracks.isEmpty()) {
            final URL url = makeUri("https://api.spotify.com/v1/playlists/"
                    + playlist.getId() + "/tracks").toURL();
            doIntervals(MAXIMUM_TRACK_IDS_REQUEST, tracks,
                    (list, i) -> addTracksFromSubList(url, playlist, list, atPosition + i), progressTracker);
        }
    }

    private void removeTracksFromSubList(URL url, Playlist playlist, List<Track> tracks)
    throws IOException, SpotifyClientException
    {
        StringBuilder body = new StringBuilder("{\"tracks\":[{\"uri\":\"spotify:track:");
        Iterator<Track> it = tracks.iterator();
        while (true) {
            body.append(it.next().getId());
            if (it.hasNext()) body.append("\"},{\"uri\":\"spotify:track:");
            else break;
        }
        body.append("\"}],\"snapshot_id\":\"");
        body.append(playlist.getSnapshotId());
        body.append("\"}");

        try (BufferedReader reader = getAPIReader(url, "DELETE", body.toString())) {
            playlist.setSnapshotId(mapper.readTree(reader).get("snapshot_id").asText());
        }
    }

    /**
     * Does not remove track in playlist object
     */
    public synchronized void removeTracksFromPlaylist(final Playlist playlist, final List<Track> tracks,
                                                      final ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        if (!tracks.isEmpty()) {
            final URL url = makeUri("https://api.spotify.com/v1/playlists/"
                    + playlist.getId() + "/tracks").toURL();
            doIntervals(MAXIMUM_TRACK_IDS_REQUEST, tracks,
                    (list) -> removeTracksFromSubList(url, playlist, list), progressTracker);
        }
    }

    private void modifySavedTracksFromSubList(List<Track> tracks, boolean shouldSave)
    throws IOException, SpotifyClientException
    {
        StringBuilder body = new StringBuilder("{\"ids\":[\"");
        Iterator<Track> it = tracks.iterator();
        while (true) {
            body.append(it.next().getId());
            if (it.hasNext()) body.append("\",\"");
            else break;
        }
        body.append("\"]}");
        getAPIReader(SAVED_TRACKS_URL, shouldSave ? "PUT" : "DELETE", body.toString()).close();
    }

    private void saveTracksFromSubList(List<Track> tracks)
    throws IOException, SpotifyClientException
    {
        modifySavedTracksFromSubList(tracks, true);
    }

    private void unsaveTracksFromSubList(List<Track> tracks)
    throws IOException, SpotifyClientException
    {
        modifySavedTracksFromSubList(tracks, false);
    }

    public synchronized void saveTracks(List<Track> tracks, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        if (!tracks.isEmpty()) {
            doIntervals(MAXIMUM_SAVED_RESOURCES_REQUEST, tracks, this::saveTracksFromSubList, progressTracker);
        }
    }

    public synchronized void unsaveTracks(List<Track> tracks, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        if (!tracks.isEmpty()) {
            doIntervals(MAXIMUM_SAVED_RESOURCES_REQUEST, tracks, this::unsaveTracksFromSubList, progressTracker);
        }
    }

    public synchronized void populateSavedTracks(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        String apiUrl = "https://api.spotify.com/v1/me/tracks?limit=50";
        JsonNode root;
        JsonNode items;
        int size = 0;
        float progress = 0;
        progressTracker.updateProgress(0);
        SavedResourceCollection<Track> ls = library.getLikedSongs();

        ls.clearResources();
        do {
            root = apiToTree(makeUri(apiUrl));
            if (size == 0) {
                if ((size = root.get("total").asInt(0)) == 0) {
                    break;
                }
            }
            if ((items = root.get("items")) != null) {
                treeToSavedTrackCollection(items, ls);
                progress += (float) items.size() / size * 100;
                progressTracker.updateProgress((int) progress);
            }
            apiUrl = (root = root.get("next")) != null ? root.asText(null) : null;
        } while (apiUrl != null);
        ls.reverse();
        library.markContentsModified(ls);
        progressTracker.updateProgress(100);
    }

    public synchronized void populateSavedAlbums(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        String apiUrl = "https://api.spotify.com/v1/me/albums?limit=50";
        JsonNode root;
        JsonNode items;
        int size = 0;
        float progress = 0;
        progressTracker.updateProgress(0);
        SavedResourceCollection<Album> sa = library.getSavedAlbums();

        do {
            root = apiToTree(makeUri(apiUrl));
            if (size == 0) {
                if ((size = root.get("total").asInt(0)) == 0) {
                    break;
                }
            }
            items = root.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode savedAlbumNode : items) {
                    library.saveResourceToCollection(treeToAlbum(savedAlbumNode.get("album"), true),
                            ZonedDateTime.parse(savedAlbumNode.get("added_at").asText()), sa);
                }
                progress += (float) items.size() / size * 100;
                progressTracker.updateProgress((int) progress);
            }
            apiUrl = (root = root.get("next")) != null ? root.asText(null) : null;
        } while (apiUrl != null);
        progressTracker.updateProgress(100);
    }

    /**
     * Downloads metadata for all the user's playlists except Liked Songs to the library, and deletes from the library
     * playlists which were deleted from Spotify.
     *
     * @throws IOException                on I/O errors
     * @throws SpotifyClientHttpException if there is an unexpected HTTP error when communicating with Spotify
     */
    public synchronized Set<Playlist> updateMyPlaylists(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        JsonNode items;
        JsonNode root;
        String nextUrl = "https://api.spotify.com/v1/me/playlists";
        Set<Playlist> deleted = new HashSet<>();
        library.getPlaylists(deleted);
        int size = 0;
        float progress = 0;
        progressTracker.updateProgress(0);
        do {
            root = apiToTree(makeUri(nextUrl));
            if (size == 0) {
                if ((size = root.get("total").asInt(0)) == 0) {
                    break;
                }
            }
            items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode node : items) {
                    deleted.remove(treeToPlaylist(node));
                }
                progress += (float) items.size() / size * 100;
                progressTracker.updateProgress((int) progress);
            }
            nextUrl = (root = root.get("next")) != null ? root.asText(null) : null;
        } while (nextUrl != null);
        progressTracker.updateProgress(100);
        deleted.forEach(library::deletePlaylist);
        return deleted;
    }

    public synchronized void updateAudioFeaturesForTrack(Track track)
    throws IOException, SpotifyClientException
    {
        treeToAudioFeatures(apiToTree(makeUri("https://api.spotify.com/v1/audio-features/" + track.getId())));
    }

    /**
     * Updates the features of all tracks.
     *
     * @throws IOException                on I/O errors
     * @throws SpotifyClientHttpException if there is an unexpected HTTP error when communicating with Spotify
     */
    public synchronized void updateAudioFeaturesForTracks(List<Track> tracks, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        doIntervals(MAXIMUM_TRACK_IDS_REQUEST, tracks, (trackSub) -> {
            JsonNode audioFeatures = apiToTree(makeUri(
                    "https://api.spotify.com/v1/audio-features?ids="
                            + joinIds(trackSub)
            )).get("audio_features");
            if (audioFeatures.isArray()) {
                for (JsonNode node : audioFeatures) {
                    treeToAudioFeatures(node);
                }
            }
        }, progressTracker);
    }

    public synchronized void updateTracks(List<Track> tracks, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        doIntervals(MAXIMUM_TRACK_IDS_REQUEST, tracks, (trackSub) -> {
            JsonNode tracksNode = apiToTree(makeUri(
                    "https://api.spotify.com/v1/tracks?ids="
                            + joinIds(trackSub)
            )).get("tracks");
            if (tracksNode.isArray()) {
                for (JsonNode trackNode : tracksNode) {
                    treeToTrack(trackNode, true, null);
                }
            }
        }, progressTracker);
    }

    public synchronized void updateTrack(Track track)
    throws IOException, SpotifyClientException
    {
        treeToTrack(apiToTree(makeUri("https://api.spotify.com/v1/tracks/" + track.getId())), true, null);
    }

    public synchronized void updateArtist(Artist artist)
    throws IOException, SpotifyClientException
    {
        treeToArtist(apiToTree(makeUri("https://api.spotify.com/v1/artists/" + artist.getId())), true);
    }

    public synchronized void updateArtists(List<Artist> artists, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        doIntervals(MAXIMUM_ARTIST_IDS_REQUEST, artists, (artistSub) -> {
            JsonNode artistsNode = apiToTree(makeUri(
                    "https://api.spotify.com/v1/artists?ids=" + joinIds(artistSub))).get("artists");
            if (artistsNode.isArray()) {
                for (JsonNode node : artistsNode) {
                    treeToArtist(node, true);
                }
            }
        }, progressTracker);
    }

    public synchronized void updateAlbum(Album album)
    throws IOException, SpotifyClientException
    {
        treeToAlbum(apiToTree(makeUri("https://api.spotify.com/v1/albums/" + album.getId())), true);
    }

    public synchronized void updateAlbums(List<Album> albums, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        doIntervals(MAXIMUM_ALBUM_IDS_REQUEST, albums, (albumSub) -> {
            JsonNode albumsNode = apiToTree(makeUri("https://api.spotify.com/v1/albums?ids="
                    + joinIds(albumSub))).get("albums");
            if (albumsNode.isArray()) {
                for (JsonNode node : albumsNode) {
                    treeToAlbum(node, true);
                }
            }
        }, progressTracker);
    }

    private URL[] treeToImages(JsonNode node)
    {
        if (node != null && node.isArray() && !node.isEmpty()) {
            URL[] imgs = new URL[node.size()];
            int i = 0;
            for (JsonNode imgNode : node) {
                try {
                    imgs[i] = new URI(imgNode.get("url").asText()).toURL();
                    i++;
                } catch (MalformedURLException | URISyntaxException ignored) {
                }
            }
            if (i != 0) return imgs;
        }
        return null;
    }

    private synchronized Artist treeToArtist(JsonNode node, boolean shouldUpdate)
    throws IOException
    {
        if (node == null) return null;
        Artist a = library.artistOf(node.get("id").asText());
        if (shouldUpdate || a.getName() == null) {
            mapper.readerForUpdating(a).readValue(node);
            Genre[] genres;
            if (node.has("genres") && (genres = treeToGenres(node.get("genres"))) != null) {
                a.setGenres(genres);
            }

            URL[] imgs = treeToImages(node.get("images"));
            if (imgs != null) a.setImages(imgs);

            if (node.has("followers")) {
                a.setFollowers(node.get("followers").get("total").asInt(0));
            }
            library.markModified(a);
        }
        return a;
    }

    private synchronized Genre[] treeToGenres(JsonNode node)
    {
        if (node != null && node.isArray() && !node.isEmpty()) {
            Genre[] genres = new Genre[node.size()];
            int i = 0;
            for (JsonNode genreNode : node) {
                genres[i++] = library.genreOf(genreNode.asText());
            }
            return genres;
        }
        return null;
    }

    private synchronized Artist[] treeToArtists(JsonNode node)
    throws IOException
    {
        if (node != null && node.isArray() && !node.isEmpty()) {
            Artist[] artists = new Artist[node.size()];
            int i = 0;
            for (JsonNode artistNode : node) {
                artists[i++] = treeToArtist(artistNode, false);
            }
            return artists;
        }
        return null;
    }

    private void mapAlbumReleaseDate(Album a, JsonNode node)
    {
        if (node == null) return;
        JsonNode precisionNode = node.get("release_date_precision");
        if ((node = node.get("release_date")) != null && precisionNode != null) {
            String precision = precisionNode.asText();
            if (precision != null) {
                String dateText = node.asText();
                switch (precision) {
                    case "day" -> a.setReleaseDate(LocalDate.parse(dateText));
                    case "month" -> a.setReleaseDate(YearMonth.parse(dateText));
                    case "year" -> a.setReleaseDate(Year.parse(dateText));
                }
            }
        }
    }

    private synchronized Track[] treeToAlbumTracks(JsonNode tracksNode, Album a)
    throws IOException, SpotifyClientException
    {
        if (tracksNode != null) {
            Track[] tracks = new Track[tracksNode.get("total").asInt(0)];
            int i = 0;
            String apiUrl = null;
            do {
                if (apiUrl != null) {
                    tracksNode = apiToTree(makeUri(apiUrl));
                }
                for (JsonNode trackListNode : tracksNode.get("items")) {
                    tracks[i++] = treeToTrack(trackListNode, false, a);
                }
                apiUrl = (tracksNode = tracksNode.get("next")) != null ? tracksNode.asText(null) : null;
            } while (apiUrl != null);
            if (i != 0) {
                return tracks;
            }
        }
        return null;
    }

    private synchronized Album treeToAlbum(JsonNode albumNode, boolean shouldUpdate)
    throws IOException, SpotifyClientException
    {
        if (albumNode == null) return null;
        Album a = library.albumOf(albumNode.get("id").asText());
        if (shouldUpdate || a.getName() == null) {
            ObjectReader reader = mapper.readerForUpdating(a);
            reader.readValue(albumNode);
            mapAlbumReleaseDate(a, albumNode);

            a.setArtists(treeToArtists(albumNode.get("artists")));
            a.setTracks(treeToAlbumTracks(albumNode.get("tracks"), a));
            a.setGenres(treeToGenres(albumNode.get("genres")));
            a.setImages(treeToImages(albumNode.get("images")));
            a.setLabel(treeToLabel(albumNode.get("label")));

            JsonNode extIdNode = albumNode.get("external_ids");
            if (extIdNode != null) {
                reader.readValue(extIdNode);
            }
            library.markModified(a);
        }
        return a;
    }

    private synchronized Label treeToLabel(JsonNode node)
    {
        if (node != null && node.isTextual()) {
            return library.labelOf(node.asText());
        }
        return null;
    }

    /**
     * Explicitly updates or creates the <code>Track</code> in the database according to <code>trackNode</code>.
     *
     * @param trackNode Spotify API node corresponding to a track
     * @return the new or existing <code>Track</code>
     * @throws JsonProcessingException on JSON errors
     */
    private synchronized Track treeToTrack(JsonNode trackNode, boolean shouldUpdate, Album album)
    throws IOException, SpotifyClientException
    {
        if (trackNode == null || (trackNode.has("is_local") && trackNode.get("is_local").asBoolean(false))
                || !trackNode.has("id")) return null;
        Track t = library.trackOf(trackNode.get("id").asText());
        if (shouldUpdate || t.getName() == null) {
            mapper.readerForUpdating(t).readValue(trackNode);
            if (album == null) {
                if ((album = treeToAlbum(trackNode.get("album"), false)) != null) t.setAlbum(album);
            } else {
                t.setAlbum(album);
            }
            Artist[] artists = treeToArtists(trackNode.get("artists"));
            if (artists != null) t.setArtists(artists);
            library.markModified(t);
        }
        return t;
    }

    /**
     * Updates the <code>AudioFeatures</code> of a <code>Track</code> in the database according to <code>node</code>.
     *
     * @param node Spotify API node corresponding to a track's features
     * @throws JsonProcessingException on JSON errors
     */
    private synchronized void treeToAudioFeatures(JsonNode node)
    throws JsonProcessingException
    {
        if (node == null) return;
        AudioFeatures features = mapper.treeToValue(node, AudioFeatures.class);
        Track t = library.trackOf(node.get("id").asText());
        t.setFeatures(features);
        library.markModified(t);
    }

    /**
     * Adds the tracks in <code>items</code> to <code>c</code>.
     *
     * @param items Spotify API node corresponding playlist tracks
     * @param c     <code>SavedResourceCollection</code> to add the tracks to
     * @throws JsonProcessingException on JSON errors
     */
    private synchronized void treeToSavedTrackCollection(JsonNode items, SavedResourceCollection<Track> c)
    throws IOException, SpotifyClientException
    {
        if (items == null) return;
        if (items.isArray()) {
            JsonNode trackNode;
            Track track;
            for (JsonNode node : items) {
                trackNode = node.get("track");
                if (!trackNode.has("added_at")) {
                    track = treeToTrack(trackNode, false, null);
                    if (track != null) {
                        library.saveResourceToCollection(track,
                                ZonedDateTime.parse(node.get("added_at").asText()), c);
                    }
                }
            }
        }
    }

    /**
     * Adds the playlist metadata represented by <code>node</code> to the library.
     *
     * @param node Spotify API node corresponding to playlist metadata
     * @throws JsonProcessingException on JSON errors
     */
    private synchronized Playlist treeToPlaylist(JsonNode node)
    throws IOException
    {
        if (node == null) return null;
        Playlist p = library.playlistOf(node.get("id").asText());
        mapper.readerForUpdating(p).readValue(node);
        library.markModified(p);
        return p;
    }

    private JsonNode apiToTree(URI apiUrl)
    throws IOException, SpotifyClientException
    {
        try (BufferedReader reader = getAPIReader(apiUrl)) {
            return mapper.readTree(reader);
        }
    }

    private static <T> void doIntervals(final int intervalSize, List<T> list, ListIntervalProcessor<T> task,
                                        ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        final int length = list.size();
        int actualEnd;
        progressTracker.updateProgress(0);
        for (int start = 0, end = intervalSize;
             start < length; start = end, end += intervalSize) {
            if (start == end) break;
            actualEnd = Math.min(end, length);
            task.process(list.subList(start, actualEnd));
            progressTracker.updateProgress((int) ((float) actualEnd / length * 100));
        }
        progressTracker.updateProgress(100);
    }

    private static <T> void doIntervals(final int intervalSize, List<T> list, ContextualListIntervalProcessor<T> task,
                                        ProgressTracker progressTracker)
    throws IOException, SpotifyClientException
    {
        final int length = list.size();
        int actualEnd;
        progressTracker.updateProgress(0);
        for (int start = 0, end = intervalSize;
             start < length; start = end, end += intervalSize) {
            if (start == end) break;
            actualEnd = Math.min(end, length);
            task.process(list.subList(start, actualEnd), start);
            progressTracker.updateProgress((int) ((float) actualEnd / length * 100));
        }
        progressTracker.updateProgress(100);
    }

    private static URI makeUri(String path)
    throws SpotifyAPIResponseException
    {
        try {
            return new URI(path);
        } catch (URISyntaxException e) {
            throw new SpotifyAPIResponseException(e);
        }
    }

    private static <T extends AbstractSpotifyResource> String joinIds(List<T> list)
    {
        return list.stream().map(AbstractSpotifyResource::getId).collect(Collectors.joining(","));
    }
}
