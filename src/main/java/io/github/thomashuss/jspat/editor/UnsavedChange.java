package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;
import io.github.thomashuss.jspat.library.SavedResource;

public class UnsavedChange<T extends AbstractSpotifyResource>
        extends Change<T>
{
    T target;

    UnsavedChange(T target)
    {
        this.target = target;
    }

    @Override
    SavedResource<T> getSavedResource()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    T getResource()
    {
        return target;
    }
}
