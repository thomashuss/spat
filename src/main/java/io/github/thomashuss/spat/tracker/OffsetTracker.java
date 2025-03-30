package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

abstract class OffsetTracker
{
    private static final int THRESHOLD = 8;

    abstract int get(int i);

    abstract void adjustOffset(int lower, int upper, int offset);

    static <T extends AbstractSpotifyResource> OffsetTracker of(List<Change<T>> removals,
                                                                List<Change<T>> additions, int n)
    {
        Iterator<Change<?>> it = new Iterator<>()
        {
            private final Iterator<Change<T>> rit = maybeGetIterator(removals);
            private final Iterator<Change<T>> ait = maybeGetIterator(additions);
            private Change<T> nextR = rit.hasNext() ? rit.next() : null;
            private Change<T> nextA = ait.hasNext() ? ait.next() : null;

            @Override
            public boolean hasNext()
            {
                return nextR != null || nextA != null;
            }

            @Override
            public Change<T> next()
            {
                Change<T> ret;
                int ri = Integer.MAX_VALUE, ai = Integer.MAX_VALUE;
                if (nextR != null) ri = nextR.oldIdx;
                if (nextA != null) ai = nextA.newIdx;
                if (ri <= ai) {
                    ret = nextR;
                    nextR = rit.hasNext() ? rit.next() : null;
                } else {
                    ret = nextA;
                    nextA = ait.hasNext() ? ait.next() : null;
                }
                return ret;
            }
        };

        if (it.hasNext()) {
            int[] indices = new int[n];
            Change<?> next = it.next();
            int acc = 0;
            for (int j = 0; j < indices.length; j++) {
                while (next != null && Math.max(next.oldIdx, next.newIdx) == j) {
                    acc += Change.getChangeType(next);
                    if (it.hasNext()) next = it.next();
                    else next = null;
                }
                indices[j] = j + acc;
            }
            return of(indices);
        }
        return of(getSequentialIndices(n));
    }

    private static OffsetTracker of(int[] a)
    {
        if (a.length < THRESHOLD) {
            return new ArrayOffsetTracker(a);
        } else {
            return new FastArrayOffsetTracker(a);
        }
    }

    private static <T> Iterator<T> maybeGetIterator(List<T> l)
    {
        return l == null ? new Iterator<>()
        {
            @Override
            public boolean hasNext()
            {
                return false;
            }

            @Override
            public T next()
            {
                throw new NoSuchElementException();
            }
        } : l.iterator();
    }

    private static int[] getSequentialIndices(int n)
    {
        int[] a = new int[n];
        Arrays.setAll(a, i -> i);
        return a;
    }

    private static class ArrayOffsetTracker
            extends OffsetTracker
    {
        private final int[] indices;

        private ArrayOffsetTracker(int[] indices)
        {
            this.indices = indices;
        }

        @Override
        int get(int i)
        {
            return indices[i];
        }

        @Override
        void adjustOffset(int lower, int upper, int offset)
        {
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] >= lower && indices[i] < upper) {
                    indices[i] += offset;
                }
            }
        }
    }

    private static class FastArrayOffsetTracker
            extends OffsetTracker
    {
        private final int[] indices;
        private final ArrayList<Integer> fences;

        private FastArrayOffsetTracker(int[] indices)
        {
            this.indices = indices;
            fences = new ArrayList<>();
            fences.add(0);
        }

        private int findRangeInFence(int fenceLower, int fenceUpper,
                                     int rangeLower, int rangeUpper)
        {
            int low = fenceLower;
            int high = fenceUpper - 1;
            int mid, val;
            while (low <= high) {
                val = indices[mid = (low + high) >>> 1];
                if (val < rangeLower) low = mid + 1;
                else if (val >= rangeUpper) high = mid - 1;
                else return mid;
            }
            return low;
        }

        @Override
        int get(int i)
        {
            return indices[i];
        }

        @Override
        void adjustOffset(int lower, int upper, int offset)
        {
            int fenceLower, fenceUpper, rangeIdx, curr, prev, j;
            for (int fup = 1; fup <= fences.size(); fup++) {
                fenceLower = fences.get(fup - 1);
                fenceUpper = fup == fences.size() ? indices.length : fences.get(fup);
                if (indices[fenceLower] >= upper || indices[fenceUpper - 1] < lower) continue;
                rangeIdx = findRangeInFence(fenceLower, fenceUpper, lower, upper);

                for (j = rangeIdx; j < fenceUpper && indices[j] >= lower && indices[j] < upper; j++) {
                    curr = indices[j] += offset;
                    if (j - 1 >= rangeIdx) {
                        prev = indices[j - 1];
                        if ((prev < lower || prev >= upper) && curr < prev) {
                            fences.add(fup++, j);
                        }
                    }
                }
                if (j < fenceUpper && j - 1 >= rangeIdx) {
                    curr = indices[j];
                    prev = indices[j - 1];
                    if ((prev < lower || prev >= upper) && prev > curr) {
                        fences.add(fup++, j);
                    }
                }
                for (j = rangeIdx - 1; j >= fenceLower && indices[j] >= lower && indices[j] < upper; j--) {
                    curr = indices[j] += offset;
                    if (j + 1 < fenceUpper) {
                        prev = indices[j + 1];
                        if (curr > prev) {
                            fences.add(fup++, j + 1);
                        }
                    }
                }
            }
        }
    }
}
