package io.github.thomashuss.spat.tracker;

import java.util.AbstractList;
import java.util.List;

class ReversedList<T>
        extends AbstractList<T>
{
    private final List<T> list;

    ReversedList(List<T> list)
    {
        this.list = list;
    }

    @Override
    public T get(int i)
    {
        return list.get(list.size() - i - 1);
    }

    @Override
    public int size()
    {
        return list.size();
    }
}
