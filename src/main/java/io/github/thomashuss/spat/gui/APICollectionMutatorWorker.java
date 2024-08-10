package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APICollectionMutator;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.LibraryResource;

import java.io.IOException;
import java.util.Set;

class APICollectionMutatorWorker<T extends LibraryResource>
        extends APIWorker<Set<T>, Void>
{
    private final APICollectionMutator<T> task;

    public APICollectionMutatorWorker(MainGUI main, APICollectionMutator<T> task)
    {
        super(main);
        this.task = task;
    }

    @Override
    protected Set<T> doTask()
    throws IOException, SpotifyClientException
    {
        return task.apply(this);
    }
}
