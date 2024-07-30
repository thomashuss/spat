package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APILongFunction;
import io.github.thomashuss.spat.client.SpotifyAuthenticationException;
import io.github.thomashuss.spat.client.SpotifyClientHttpException;
import io.github.thomashuss.spat.client.SpotifyClientStateException;

import java.io.IOException;
import java.net.URISyntaxException;

class APILongFunctionWorker<T>
        extends APIWorker<Void>
{
    protected final T t;
    private final APILongFunction<T> task;

    public APILongFunctionWorker(MainGUI main, APILongFunction<T> task, T t)
    {
        super(main);
        this.task = task;
        this.t = t;
    }

    @Override
    protected Void doTask()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException
    {
        task.apply(t, this);
        return null;
    }
}
