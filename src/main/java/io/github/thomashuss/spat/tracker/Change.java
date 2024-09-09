package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import javax.annotation.Nonnull;

final class Change<T extends AbstractSpotifyResource>
    implements Comparable<Change<T>>
{
    static final int REMOVE = -1;
    static final int ADD = 1;
    static final int MOVE = 2;
    final T target;
    byte seen = 0;
    int oldIdx;
    int newIdx = -1;

    Change(T target)
    {
        this.target = target;
    }

    static <T extends AbstractSpotifyResource> int getChangeType(Change<T> a)
    {
        if (a.newIdx == -1) return REMOVE;
        else if (a.oldIdx == -1) return ADD;
        else return MOVE;
    }

    T getTarget()
    {
        return target;
    }

    int getOldIdx()
    {
        return oldIdx;
    }

    @Override
    public int compareTo(@Nonnull Change<T> r)
    {
        return (getChangeType(this) & getChangeType(r)) < 1 ? oldIdx - r.oldIdx : newIdx - r.newIdx;
    }
}
