package io.github.thomashuss.spat.tracker;

import java.util.List;

public interface TrackRemoval
{
    List<Integer> indices();

    boolean isSequential();
}
