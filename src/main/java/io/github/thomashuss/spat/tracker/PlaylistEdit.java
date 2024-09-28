package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;

public abstract class PlaylistEdit
    extends Edit
{
    protected final Playlist playlist;

    PlaylistEdit(Playlist playlist)
    {
        this.playlist = playlist;
    }

    @Override
    public LibraryResource getTarget()
    {
        return playlist;
    }

    @Override
    void mark(Library library)
    {
        library.markContentsModified(playlist);
    }

    @Override
    void unmark(Library library)
    {
        library.unmarkContentsModified(playlist);
    }
}
