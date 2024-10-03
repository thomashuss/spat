package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public abstract class ResourceFilter<T extends AbstractSpotifyResource>
        extends Edit
{
    protected final Library library;
    private ArrayList<Change<T>> changes;
    private Edit head;
    private Edit last;

    public ResourceFilter(Library library)
    {
        this.library = library;
    }

    protected void enqueue(Edit edit)
    {
        if (edit.seen) {
            throw new RuntimeException("Edit already seen");
        }
        edit.seen = true;

        if (head == null || last == null) {
            head = edit;
        }
        if (last != null) {
            last.next = edit;
            edit.prev = last;
        }
        last = edit;
    }

    public void forEach(Consumer<Edit> func, boolean isCommit)
    {
        if (isCommit) for (Edit e = head; e != null; e = e.next) func.accept(e);
        else for (Edit e = last; e != null; e = e.prev) func.accept(e);
    }

    abstract T getByKey(String key);

    abstract void remove(List<Change<T>> removals, boolean isSequential);

    abstract void add(List<Change<T>> additions);

    abstract boolean supportsMove();

    abstract void move(List<Change<T>> range);

    @Override
    public abstract SavedResourceCollection<T> getTarget();

    @Override
    void commit(Library library)
    {
        for (Edit e = head; e != null; e = e.next) {
            e.commit(library);
        }
    }

    @Override
    void mark(Library library)
    {
        library.markContentsModified(getTarget());
    }

    @Override
    void unmark(Library library)
    {
        library.unmarkContentsModified(getTarget());
    }

    @Override
    void revert(Library library)
    {
        for (Edit e = last; e != null; e = e.prev) {
            e.revert(library);
        }
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        final long cooldown = Spat.preferences.getLong(Spat.P_PUSH_COOLDOWN, 500);
        try {
            Edit e = head;
            while (e != null) {
                e.push(client, progressTracker);
                e = e.next;
                if (e != null) Thread.sleep(cooldown);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Change<T> addChange(T t)
    {
        Change<T> c = new Change<>(t);
        changes.add(c);
        return c;
    }

    public final void filter(List<T> filtered, boolean shouldFailOnDuplicates)
    throws IllegalEditException
    {
        SavedResourceCollection<T> target = getTarget();
        if (target.isEmpty() || filtered.isEmpty()) return;
        HashMap<T, Change<T>> changeForResource = new HashMap<>();
        changes = new ArrayList<>();
        int i;
        Change<T> change;

        final int srLength = target.getNumResources();
        i = 0;
        for (SavedResource<T> sr : target.getSavedResources()) {
            change = changeForResource.computeIfAbsent(sr.getResource(), this::addChange);
            if ((change.seen & 1) == 1) {
                if (shouldFailOnDuplicates) {
                    throw new IllegalEditException(target,
                            "Element `" + change.target + "' was duplicated in the original list");
                }
            } else {
                change.seen = 1;
                change.oldIdx = i;
            }
            i++;
        }

        i = 0;
        for (T t : filtered) {
            change = changeForResource.computeIfAbsent(t, this::addChange);
            if ((change.seen & 2) == 2) {
                if (shouldFailOnDuplicates) {
                    throw new IllegalEditException(target,
                            "Element `" + change.target + "' was duplicated in the filtered list");
                }
            } else {
                change.seen += 2;
                change.newIdx = i++;
            }
        }

        changes.sort(null);
        final int length = changes.size();

        i = 0;
        final List<Change<T>> r;
        final List<Change<T>> a;
        if ((change = changes.get(0)).newIdx == -1) {
            int prev = change.oldIdx;
            boolean isSequential = true;
            for (i = 1; i < length && (change = changes.get(i)).newIdx == -1; i++) {
                if (isSequential && (isSequential = prev - 1 == change.oldIdx)) {
                    prev = change.oldIdx;
                }
            }
            remove(r = changes.subList(0, i), isSequential);
        } else r = null;

        if (i < length && changes.get(i).oldIdx == -1) {
            int leftBound = i;
            for (i++; i < length && changes.get(i).oldIdx == -1; i++) ;
            add(a = changes.subList(leftBound, i));
        } else a = null;

        if (supportsMove() && i < length) {
            new MoveIterator<>(changes.subList(i, length), OffsetTracker.of(r, a, srLength))
                    .forEachRemaining(this::move);
        }
    }

    @Override
    public String toString()
    {
        return "Apply filter to " + getTarget();
    }
}
