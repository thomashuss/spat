package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResource;

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
