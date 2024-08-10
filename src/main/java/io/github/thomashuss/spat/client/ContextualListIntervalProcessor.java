package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
interface ContextualListIntervalProcessor<T>
{
    void process(List<T> list, int i) throws IOException, SpotifyClientException;
}
