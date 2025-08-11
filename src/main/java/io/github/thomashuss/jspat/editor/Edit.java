package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.client.ProgressTracker;
import io.github.thomashuss.jspat.client.SpotifyClient;
import io.github.thomashuss.jspat.client.SpotifyClientException;
import io.github.thomashuss.jspat.library.Library;
import io.github.thomashuss.jspat.library.LibraryResource;

import java.io.IOException;

public abstract class Edit
{
    Edit prev;
    Edit next;

    public abstract LibraryResource getTarget();

    abstract void commit(Library library);

    abstract void mark(Library library);

    abstract void unmark(Library library);

    abstract void revert(Library library);

    abstract void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException;
}
