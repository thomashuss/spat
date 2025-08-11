package io.github.thomashuss.jspat.client;

import java.io.IOException;

@FunctionalInterface
public interface APIBiFunction<T, U>
{
    void apply(T t, U u)
    throws IOException, SpotifyClientException;
}
