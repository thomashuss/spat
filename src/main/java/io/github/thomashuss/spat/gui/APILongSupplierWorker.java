package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APILongSupplier;
import io.github.thomashuss.spat.client.SpotifyAuthenticationException;
import io.github.thomashuss.spat.client.SpotifyClientHttpException;
import io.github.thomashuss.spat.client.SpotifyClientStateException;

import java.io.IOException;
import java.net.URISyntaxException;

class APILongSupplierWorker
        extends APIWorker<Void>
{
    private final APILongSupplier task;

    public APILongSupplierWorker(MainGUI main, APILongSupplier task)
    {
        super(main);
        this.task = task;
    }

    @Override
    protected Void doTask()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException
    {
        task.apply(this);
        return null;
    }
}
