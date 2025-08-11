package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;
import io.github.thomashuss.jspat.library.Library;
import io.github.thomashuss.jspat.library.LibraryResource;
import io.github.thomashuss.jspat.library.SavedResourceCollection;

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
