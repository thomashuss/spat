package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.Playlist;

import java.io.IOException;

public class MoveTracks
        extends PlaylistEdit
{
    public final int insertBefore;
    public final int rangeStart;
    public final int rangeLength;

    MoveTracks(Playlist playlist, int insertBefore,
                      int rangeStart, int rangeLength)
    {
        super(playlist);
        this.insertBefore = insertBefore;
        this.rangeStart = rangeStart;
        this.rangeLength = rangeLength;
    }

    public static MoveTracks of(Playlist playlist, int insertBefore,
                                int rangeStart, int rangeLength)
    {
        return new MoveTracks(playlist, insertBefore, rangeStart, rangeLength);
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
