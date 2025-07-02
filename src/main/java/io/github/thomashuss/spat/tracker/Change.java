package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResource;

import javax.annotation.Nonnull;

abstract class Change<T extends AbstractSpotifyResource>
        implements Comparable<Integer>
{
    int oldIdx = -1;
    int newIdx = -1;

    abstract SavedResource<T> getSavedResource();

    abstract T getResource();

    int getOldIdx()
    {
        return oldIdx;
    }

    static int compareOld(Change<?> l, Change<?> r)
    {
        return l.oldIdx - r.oldIdx;
    }

    @Override
    public int compareTo(@Nonnull Integer i)
    {
        return newIdx - i;
    }
}
