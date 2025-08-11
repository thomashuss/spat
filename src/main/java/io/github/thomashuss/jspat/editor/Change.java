package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;
import io.github.thomashuss.jspat.library.SavedResource;

import javax.annotation.Nonnull;

abstract class Change<T extends AbstractSpotifyResource>
        implements Comparable<Integer>
{
    int oldIdx = -1;
    int newIdx = -1;

    static int compareOld(Change<?> l, Change<?> r)
    {
        return l.oldIdx - r.oldIdx;
    }

    abstract SavedResource<T> getSavedResource();

    abstract T getResource();

    int getOldIdx()
    {
        return oldIdx;
    }

    @Override
    public int compareTo(@Nonnull Integer i)
    {
        return newIdx - i;
    }
}
