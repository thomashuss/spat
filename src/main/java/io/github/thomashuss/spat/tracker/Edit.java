package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;

import java.io.IOException;

public abstract class Edit
{
    Edit prev;
    Edit next;
    boolean seen = false;

    public abstract LibraryResource getTarget();

    abstract void commit(Library library);

    abstract void mark(Library library);

    abstract void unmark(Library library);

    abstract void revert(Library library);

    abstract void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException;
}
