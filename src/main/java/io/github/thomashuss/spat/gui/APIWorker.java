package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyAuthenticationException;
import io.github.thomashuss.spat.client.SpotifyClientHttpException;
import io.github.thomashuss.spat.client.SpotifyClientStateException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

abstract class APIWorker<T>
        extends SwingWorker<T, Void>
        implements ProgressTracker
{
    protected final MainGUI main;

    protected APIWorker(MainGUI main)
    {
        super();
        addPropertyChangeListener(main);
        this.main = main;
    }

    protected void onTaskSuccess(T t)
    {
    }

    @Override
    protected T doInBackground()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException
    {
        return doTask();
    }

    abstract protected T doTask()
    throws SpotifyAuthenticationException, SpotifyClientHttpException, SpotifyClientStateException, IOException, URISyntaxException;

    @Override
    protected void done()
    {
        removePropertyChangeListener(main);
        try {
            onTaskSuccess(get());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SpotifyClientHttpException
                    || cause instanceof SpotifyAuthenticationException
                    || cause instanceof SpotifyClientStateException) {
                JOptionPane.showInternalMessageDialog(main.desktopPane, "There was a problem communicating with Spotify:\n\n" + cause.getMessage(), "Spotify client error", JOptionPane.ERROR_MESSAGE);
            } else if (cause instanceof IOException || cause instanceof URISyntaxException) {
                JOptionPane.showInternalMessageDialog(main.desktopPane, "There was an I/O error:\n\n" + cause.getMessage(), "I/O error", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateProgress(int progress)
    {
        try {
            setProgress(progress);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
