package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.List;

class SequentialNewIterator<T extends AbstractSpotifyResource>
        extends RangeIterator<T>
{
    SequentialNewIterator(List<Change<T>> list)
    {
        super(list);
    }

    @Override
    boolean endOfRangeCheck(Change<T> prevC, Change<T> c)
    {
        return c.newIdx != prevC.newIdx + 1;
    }
}
