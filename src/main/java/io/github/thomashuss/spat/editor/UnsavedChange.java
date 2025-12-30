package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResource;

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
