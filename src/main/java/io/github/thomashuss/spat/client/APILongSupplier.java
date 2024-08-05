package io.github.thomashuss.spat.client;

import java.io.IOException;

@FunctionalInterface
public interface APILongSupplier
{
    void apply(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException;
}
