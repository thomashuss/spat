package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;

public abstract class SrcEdit<T extends AbstractSpotifyResource>
    extends Edit
{
    protected final SavedResourceCollection<T> src;

    SrcEdit(SavedResourceCollection<T> src)
    {
        this.src = src;
    }

    @Override
    public LibraryResource getTarget()
    {
        return src;
    }

    @Override
    void mark(Library library)
    {
        library.markContentsModified(src);
    }

    @Override
    void unmark(Library library)
    {
        library.unmarkContentsModified(src);
    }
}
