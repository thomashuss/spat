package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class MoveIterator<T extends AbstractSpotifyResource>
        implements Iterator<List<Change<T>>>
{
    private final ReverseSequentialOldNewIterator<T> changeIt;
    private final List<Change<T>> working;

    MoveIterator(List<Change<T>> relocations, List<Change<T>> working)
    {
        this.changeIt = new ReverseSequentialOldNewIterator<>(relocations);
        this.working = working;
    }

    @Override
    public boolean hasNext()
    {
        return changeIt.hasNext();
    }

    @Override
    public List<Change<T>> next()
    {
        List<Change<T>> changeBlock = changeIt.next();
        List<Change<T>> carousel;
        List<Change<T>> touch;
        Change<T> c = changeBlock.get(changeBlock.size() - 1);
        int offset;

        if (c.newIdx > c.oldIdx) {
            offset = -changeBlock.size();
            carousel = working.subList(c.oldIdx, c.newIdx + changeBlock.size());
            touch = carousel.subList(changeBlock.size(), carousel.size());
        } else if (c.newIdx < c.oldIdx) {
            offset = changeBlock.size();
            carousel = working.subList(c.newIdx, c.oldIdx + changeBlock.size());
            touch = carousel.subList(0, carousel.size() - changeBlock.size());
        } else {
            return changeBlock;
        }
        for (Change<T> c1 : touch) {
            c1.oldIdx += offset;
        }
        Collections.rotate(carousel, offset);
        return changeBlock;
    }
}
