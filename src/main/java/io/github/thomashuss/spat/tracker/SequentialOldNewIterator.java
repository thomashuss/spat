package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.List;

class SequentialOldNewIterator<T extends AbstractSpotifyResource>
        extends RangeIterator<T>
{
    SequentialOldNewIterator(List<Change<T>> list)
    {
        super(list);
    }

    @Override
    boolean endOfRangeCheck(Change<T> prevC, Change<T> c)
    {
        return c.oldIdx != prevC.oldIdx + 1 || c.newIdx != prevC.newIdx + 1;
    }
}
