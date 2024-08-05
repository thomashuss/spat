package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.net.URISyntaxException;

@FunctionalInterface
public interface APILongSupplier
{
    void apply(ProgressTracker progressTracker)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException, SpotifyClientStateException, URISyntaxException;
}
