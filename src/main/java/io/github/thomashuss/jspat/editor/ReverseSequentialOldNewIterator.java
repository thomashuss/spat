package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;

import java.util.List;

class ReverseSequentialOldNewIterator<T extends AbstractSpotifyResource>
        extends RangeIterator<T>
{
    ReverseSequentialOldNewIterator(List<Change<T>> list)
    {
        super(list);
    }

    @Override
    boolean endOfRangeCheck(Change<T> prevC, Change<T> c)
    {
        return c.oldIdx != prevC.oldIdx - 1 || c.newIdx != prevC.newIdx - 1;
    }
}
