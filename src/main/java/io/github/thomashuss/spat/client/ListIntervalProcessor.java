package io.github.thomashuss.spat.client;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
interface ListIntervalProcessor<T>
{
    void process(List<T> list) throws IOException, SpotifyClientException;
}
