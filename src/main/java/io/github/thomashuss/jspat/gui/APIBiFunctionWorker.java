package io.github.thomashuss.jspat.gui;

import io.github.thomashuss.jspat.client.APIBiFunction;
import io.github.thomashuss.jspat.client.SpotifyClientException;

import java.io.IOException;

class APIBiFunctionWorker<T, U>
        extends APIWorker<Void, Void>
{
    private final APIBiFunction<T, U> task;
    private final T t;
    private final U u;

    public APIBiFunctionWorker(MainGUI main, APIBiFunction<T, U> task, T t, U u)
    {
        super(main);
        this.task = task;
        this.t = t;
        this.u = u;
    }

    @Override
    protected Void doTask()
    throws IOException, SpotifyClientException
    {
        task.apply(t, u);
        return null;
    }
}
