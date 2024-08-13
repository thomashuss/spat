package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

public class AddTracks
        extends Edit
        implements TrackInsertion
{
    private final Playlist playlist;
    private final ZonedDateTime addedAt;
    private final List<Track> tracks;
    private final int index;

    AddTracks(Playlist playlist, List<Track> tracks, int index)
    {
        this.playlist = playlist;
        this.tracks = tracks;
        this.index = index;
        this.addedAt = ZonedDateTime.now();
    }

    public static AddTracks of(Playlist playlist, List<Track> tracks, int index)
    throws IllegalEditException
    {
        if (playlist.containsAnyOf(tracks))
            throw new IllegalEditException(playlist, "Tracks are already saved");
        return new AddTracks(playlist, tracks, index);
    }

    @Override
    public int index()
    {
        return index;
    }

    @Override
    public List<Track> tracks()
    {
        return tracks;
    }

    @Override
    public LibraryResource getTarget()
    {
        return playlist;
    }

    @Override
    void commit(Library library)
    {
        library.saveTracksToCollection(tracks, addedAt, playlist, index);
    }

    @Override
    void revert(Library library)
    {
        for (int i = index + tracks.size() - 1; i >= index; i--) {
            playlist.removeResource(i);
        }
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.addTracksToPlaylist(playlist, tracks, index, progressTracker);
    }

    @Override
    public String toString()
    {
        final int size = tracks.size();
        return "Add " + (size == 1 ? tracks.get(0) + " to "
                : size + " tracks to ") + playlist.getName();
    }
}
