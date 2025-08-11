package io.github.thomashuss.jspat.client;

import io.github.thomashuss.jspat.library.LibraryResource;

import java.io.IOException;
import java.util.Set;

@FunctionalInterface
public interface APICollectionMutator<T extends LibraryResource>
{
    Set<T> apply(ProgressTracker progressTracker)
    throws IOException, SpotifyClientException;
}
