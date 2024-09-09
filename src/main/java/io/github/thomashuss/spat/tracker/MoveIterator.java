package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.Iterator;
import java.util.List;

final class MoveIterator<T extends AbstractSpotifyResource>
        implements Iterator<List<Change<T>>>
{
    private final SequentialOldNewIterator<T> changeIt;
    private final OffsetTracker ot;

    MoveIterator(List<Change<T>> relocations, OffsetTracker ot)
    {
        this.changeIt = new SequentialOldNewIterator<>(relocations);
        this.ot = ot;
    }

    @Override
    public boolean hasNext()
    {
        return changeIt.hasNext();
    }

    @Override
    public List<Change<T>> next()
    {
        List<Change<T>> change = changeIt.next();
        final int n = change.size();
        for (Change<T> c : change) c.oldIdx = ot.get(c.oldIdx);
        Change<T> first = change.get(0);
        if (first.oldIdx != first.newIdx && changeIt.hasNext()) {
            ot.adjustOffset(first.newIdx, first.oldIdx + n, n);
        }
        return change;
    }
}
