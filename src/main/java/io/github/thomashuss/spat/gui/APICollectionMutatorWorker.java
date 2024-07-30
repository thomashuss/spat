package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.SpotifyAuthenticationException;
import io.github.thomashuss.spat.client.SpotifyClientHttpException;
import io.github.thomashuss.spat.client.APICollectionMutator;
import io.github.thomashuss.spat.client.SpotifyClientStateException;
import io.github.thomashuss.spat.library.LibraryResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

class APICollectionMutatorWorker<T extends LibraryResource>
        extends APIWorker<Set<T>>
{
    private final APICollectionMutator<T> task;

    public APICollectionMutatorWorker(MainGUI main, APICollectionMutator<T> task)
    {
        super(main);
        this.task = task;
    }

    @Override
    protected Set<T> doTask()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException
    {
        return task.apply(this);
    }
}
