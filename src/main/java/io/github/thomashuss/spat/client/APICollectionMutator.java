package io.github.thomashuss.spat.client;

import io.github.thomashuss.spat.library.LibraryResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@FunctionalInterface
public interface APICollectionMutator<T extends LibraryResource>
{
    Set<T> apply(ProgressTracker progressTracker)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException, SpotifyClientStateException, URISyntaxException;
}
