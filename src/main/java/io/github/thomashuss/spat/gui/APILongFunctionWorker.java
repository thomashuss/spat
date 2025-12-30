package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APILongFunction;
import io.github.thomashuss.spat.client.SpotifyClientException;

import java.io.IOException;

class APILongFunctionWorker<T>
        extends APIWorker<Void, Void>
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
    throws IOException, SpotifyClientException
    {
        task.apply(t, this);
        return null;
    }
}
