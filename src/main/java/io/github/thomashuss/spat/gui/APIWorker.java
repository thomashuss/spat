package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClientException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

abstract class APIWorker<T, V>
        extends SwingWorker<T, V>
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
    throws IOException, SpotifyClientException, InterruptedException
    {
        return doTask();
    }

    abstract protected T doTask()
    throws SpotifyClientException, IOException, InterruptedException;

    @Override
    protected void done()
    {
        removePropertyChangeListener(main);
        try {
            onTaskSuccess(get());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SpotifyClientException) {
                JOptionPane.showInternalMessageDialog(main.desktopPane, "There was a problem communicating with Spotify:\n\n" + cause.getMessage(), "Spotify client error", JOptionPane.ERROR_MESSAGE);
            } else if (cause instanceof IOException) {
                JOptionPane.showInternalMessageDialog(main.desktopPane, "There was an I/O error:\n\n" + cause.getMessage(), "I/O error", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void updateProgress(int progress)
    {
        try {
            setProgress(progress);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
