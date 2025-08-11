package io.github.thomashuss.jspat.client;

import java.io.IOException;

@FunctionalInterface
public interface APILongSupplier
{
    void apply(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException;
}
