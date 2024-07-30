package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface APIFunction<T>
{
    void apply(T t)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException, SpotifyClientStateException, URISyntaxException;
}
