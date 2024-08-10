package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;

import java.io.IOException;
import java.util.function.Consumer;

public class EditTracker
{
    private Edit head;
    private Edit last;

    public void commit(Edit edit, Library library)
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
        edit.commit(library);
        last = edit;
    }

    public Edit peekUndo()
    {
        return last;
    }

    public Edit peekRedo()
    {
        return last == null ? head : last.next;
    }

    public boolean hasChanges()
    {
        return last != null;
    }

    public Edit undo(Library library)
    {
        if (last != null) {
            last.revert(library);
            Edit ret = last;
            last = last.prev;
            return ret;
        }
        return null;
    }

    public Edit redo(Library library)
    {
        Edit e = peekRedo();
        if (e != null) {
            e.commit(library);
            last = e;
        }
        return e;
    }

    public void abandonEditsFor(LibraryResource resource)
    {
        for (Edit e = head; e != null; e = e.next) {
            if (e.getTarget() == resource) {
                if (e.prev != null)
                    e.prev.next = e.next;
                if (e.next != null)
                    e.next.prev = e.prev;
                if (e == head)
                    head = e.next;
                if (e == last)
                    last = e.prev;
            }
        }
        if (last != null && last.getTarget() == resource)
            last = null;
    }

    public void forEachDescription(Consumer<String> func)
    {
        if (head != null) {
            final Edit bound = last == null ? null : last.next;
            for (Edit e = head; e != bound; e = e.next) {
                func.accept(e.toString());
            }
        }
    }

    public void pushAll(SpotifyClient client, ProgressTracker childProgressTracker,
                        ProgressTracker parentProgressTracker)
    throws SpotifyClientException, IOException, InterruptedException
    {
        if (head != null) {
            final long cooldown = Spat.preferences.getLong(Spat.P_PUSH_COOLDOWN, 500);
            Edit e = head;
            final Edit bound = last == null ? null : last.next;
            int done = 0;
            try {
                for (; e != bound; e = e.next) {
                    if (done != 0) Thread.sleep(cooldown);
                    e.push(client, childProgressTracker);
                    parentProgressTracker.updateProgress(done++);
                }
            } finally {
                head = e;
                last = null;
                if (head != null) {
                    head.prev = null;
                }
            }
        }
    }
}
