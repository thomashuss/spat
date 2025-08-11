package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.Track;

import java.util.List;

// TODO: make generic
public interface TrackInsertion
{
    int index();

    List<Track> tracks();
}
