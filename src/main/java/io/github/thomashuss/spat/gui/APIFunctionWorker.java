package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APIFunction;
import io.github.thomashuss.spat.client.SpotifyAuthenticationException;
import io.github.thomashuss.spat.client.SpotifyClientHttpException;
import io.github.thomashuss.spat.client.SpotifyClientStateException;

import java.io.IOException;
import java.net.URISyntaxException;

class APIFunctionWorker<T>
        extends APIWorker<Void>
{
    protected final T t;
    private final APIFunction<T> task;

    public APIFunctionWorker(MainGUI main, APIFunction<T> task, T t)
    {
        super(main);
        this.task = task;
        this.t = t;
    }

    @Override
    protected Void doTask()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException
    {
        task.apply(t);
        return null;
    }
}
