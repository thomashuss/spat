package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.Iterator;
import java.util.List;

abstract class RangeIterator<T extends AbstractSpotifyResource>
        implements Iterator<List<Change<T>>>
{
    private final List<Change<T>> list;
    private final int size;
    private int pos = 0;

    RangeIterator(List<Change<T>> list)
    {
        this.list = list;
        size = list.size();
    }

    @Override
    public boolean hasNext()
    {
        return pos < size;
    }

    abstract boolean endOfRangeCheck(Change<T> prevC, Change<T> c);

    @Override
    public List<Change<T>> next()
    {
        final int left = pos;
        int i = pos;
        Change<T> c = null;
        Change<T> prevC;
        do {
            if ((pos = i) < size) {
                prevC = c;
                c = list.get(i++);
            } else break;
        } while (prevC == null || !endOfRangeCheck(prevC, c));
        return list.subList(left, pos);
    }
}
