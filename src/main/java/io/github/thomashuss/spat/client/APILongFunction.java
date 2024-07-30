package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface APILongFunction<T>
{
    void apply(T t, ProgressTracker progressTracker)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException, SpotifyClientStateException, URISyntaxException;
}
