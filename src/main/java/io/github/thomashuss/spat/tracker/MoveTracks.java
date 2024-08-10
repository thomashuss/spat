package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;

import java.io.IOException;

public class MoveTracks
        extends Edit
{
    public final int insertBefore;
    public final int rangeStart;
    public final int rangeLength;
    private final Playlist playlist;

    public MoveTracks(Playlist playlist, int insertBefore,
                      int rangeStart, int rangeLength)
    {
        this.playlist = playlist;
        this.insertBefore = insertBefore;
        this.rangeStart = rangeStart;
        this.rangeLength = rangeLength;
    }

    @Override
    public LibraryResource getTarget()
    {
        return playlist;
    }

    @Override
    void commit(Library library)
    {
        playlist.move(insertBefore, rangeStart, rangeLength, true);
    }

    @Override
    void revert(Library library)
    {
        playlist.move(insertBefore, rangeStart, rangeLength, false);
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.reorderPlaylist(playlist, insertBefore, rangeStart, rangeLength);
    }

    @Override
    public String toString()
    {
        return "Move " + rangeLength + (rangeLength == 1 ? " track in " : " tracks in ") + playlist.getName();
    }
}
