package io.github.thomashuss.jspat.client;

import java.io.IOException;

@FunctionalInterface
public interface APIFunction<T>
{
    void apply(T t)
    throws IOException, SpotifyClientException;
}
