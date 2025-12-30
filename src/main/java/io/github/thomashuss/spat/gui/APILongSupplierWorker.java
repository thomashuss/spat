package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APILongSupplier;
import io.github.thomashuss.spat.client.SpotifyClientException;

import java.io.IOException;

class APILongSupplierWorker
        extends APIWorker<Void, Void>
{
    private final APILongSupplier task;

    public APILongSupplierWorker(MainGUI main, APILongSupplier task)
    {
        super(main);
        this.task = task;
    }

    @Override
    protected Void doTask()
    throws IOException, SpotifyClientException
    {
        task.apply(this);
        return null;
    }
}
