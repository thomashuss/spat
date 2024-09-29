package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.Track;

import java.util.List;

// TODO: make generic
public interface TrackInsertion
{
    int index();

    List<Track> tracks();
}
