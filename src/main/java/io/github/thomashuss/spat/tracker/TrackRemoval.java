package io.github.thomashuss.spat.tracker;

import java.util.List;

// TODO: make generic
public interface TrackRemoval
{
    List<Integer> indices();

    boolean isSequential();
}
