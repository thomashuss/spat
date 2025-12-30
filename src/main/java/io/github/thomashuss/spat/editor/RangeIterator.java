package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.Iterator;
import java.util.List;

abstract class RangeIterator<T extends AbstractSpotifyResource>
        implements Iterator<List<Change<T>>>
{
    private final List<Change<T>> list;
    private int pos = 0;

    RangeIterator(List<Change<T>> list)
    {
        this.list = list;
    }

    @Override
    public boolean hasNext()
    {
        return pos < list.size();
    }

    abstract boolean endOfRangeCheck(Change<T> prevC, Change<T> c);

    @Override
    public List<Change<T>> next()
    {
        int size = list.size();
        int i = pos;
        Change<T> prevC = null;
        Change<T> c;
        List<Change<T>> ret;
        for (; i < size; i++) {
            c = list.get(i);
            if (prevC != null && endOfRangeCheck(prevC, c)) {
                break;
            }
            prevC = c;
        }
        ret = list.subList(pos, i);
        pos = i;
        return ret;
    }
}
