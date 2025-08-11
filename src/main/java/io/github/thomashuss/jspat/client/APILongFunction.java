package io.github.thomashuss.jspat.client;

import java.io.IOException;

@FunctionalInterface
public interface APILongFunction<T>
{
    void apply(T t, ProgressTracker progressTracker)
    throws IOException, SpotifyClientException;
}
