package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.client.ProgressTracker;
import io.github.thomashuss.jspat.client.SpotifyClient;
import io.github.thomashuss.jspat.client.SpotifyClientException;
import io.github.thomashuss.jspat.library.Library;
import io.github.thomashuss.jspat.library.Playlist;

import java.io.IOException;

public class MoveTracks
        extends PlaylistEdit
{
    public final int insertAt;
    public final int rangeStart;
    public final int rangeLength;

    MoveTracks(Playlist playlist, int insertAt,
               int rangeStart, int rangeLength)
    {
        super(playlist);
        this.insertAt = insertAt;
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
        playlist.move(insertAt, rangeStart, rangeLength);
    }

    @Override
    void revert(Library library)
    {
        playlist.move(rangeStart, insertAt, rangeLength);
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.reorderPlaylist(playlist, insertAt <= rangeStart ? insertAt : insertAt + rangeLength,
                rangeStart, rangeLength);
    }

    @Override
    public String toString()
    {
        return "Move " + rangeLength + (rangeLength == 1 ? " track in " : " tracks in ") + playlist.getName();
    }
}
