package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface APIBiFunction<T, U>
{
    void apply(T t, U u)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException, SpotifyClientStateException, URISyntaxException;
}
