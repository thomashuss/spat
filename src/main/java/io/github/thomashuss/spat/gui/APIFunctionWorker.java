package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APIFunction;
import io.github.thomashuss.spat.client.SpotifyClientException;

import java.io.IOException;

class APIFunctionWorker<T>
        extends APIWorker<Void, Void>
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
    throws IOException, SpotifyClientException
    {
        task.apply(t);
        return null;
    }
}
