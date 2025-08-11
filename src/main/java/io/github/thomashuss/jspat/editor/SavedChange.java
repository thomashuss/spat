package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;
import io.github.thomashuss.jspat.library.SavedResource;

class SavedChange<T extends AbstractSpotifyResource>
        extends Change<T>
{
    SavedResource<T> target;

    SavedChange(SavedResource<T> target)
    {
        this.target = target;
    }

    @Override
    SavedResource<T> getSavedResource()
    {
        return target;
    }

    @Override
    T getResource()
    {
        return target.getResource();
    }
}
