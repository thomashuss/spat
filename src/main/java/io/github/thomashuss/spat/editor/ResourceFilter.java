package io.github.thomashuss.spat.editor;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public abstract class ResourceFilter<T extends AbstractSpotifyResource>
        extends Edit
{
    protected final Library library;
    private Edit head;
    private Edit last;

    public ResourceFilter(Library library)
    {
        this.library = library;
    }

    private static <T extends AbstractSpotifyResource> int countLessThanEq(List<Change<T>> in, int i)
    {
        int k = Collections.binarySearch(in, i);
        return k < 0 ? -k - 1 : k + 1;
    }

    protected void enqueue(Edit edit)
    {
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

    abstract void remove(List<Change<T>> removals);

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

    public final void filter(List<T> filtered)
    {
        SavedResourceCollection<T> target = getTarget();
        HashMap<T, Queue<Change<T>>> changeForResource = new HashMap<>();
        ArrayList<Change<T>> moves = supportsMove() ? new ArrayList<>() : null;
        ArrayList<Change<T>> deletions = new ArrayList<>();
        ArrayList<Change<T>> insertions = new ArrayList<>();
        Change<T> change;
        int i;

        i = 0;
        for (SavedResource<T> sr : target.getSavedResources()) {
            change = new SavedChange<>(sr);
            changeForResource.computeIfAbsent(sr.getResource(), k -> new LinkedList<>()).add(change);
            change.oldIdx = i;
            i++;
        }

        i = 0;
        for (T t : filtered) {
            Queue<Change<T>> q = changeForResource.get(t);
            if (q == null) {
                change = new UnsavedChange<>(t);
                insertions.add(change);
            } else {
                change = q.poll();
                if (change == null) {
                    change = new UnsavedChange<>(t);
                    insertions.add(change);
                } else if (moves != null) {
                    moves.add(change);
                }
            }
            change.newIdx = i++;
        }

        for (Queue<Change<T>> q : changeForResource.values()) {
            while (!q.isEmpty()) {
                deletions.add(q.poll());
            }
        }
        deletions.sort(Change::compareOld);
        if (!deletions.isEmpty()) {
            remove(deletions);
        }

        if (moves != null && !moves.isEmpty()) {
            ArrayList<Change<T>> working = new ArrayList<>(moves);
            working.sort(Change::compareOld);
            i = 0;
            for (Change<T> move : working) {
                move.oldIdx = i++;
                move.newIdx -= countLessThanEq(insertions, move.newIdx);  // monotone nondecreasing => still sorted
            }
            new MoveIterator<>(new ReversedList<>(moves), working).forEachRemaining(this::move);
        }

        if (!insertions.isEmpty()) {
            add(insertions);
        }
    }

    @Override
    public String toString()
    {
        return "Apply filter to " + getTarget();
    }
}
