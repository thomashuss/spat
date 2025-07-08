package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.util.List;

public class SavedTrackFilter
        extends TrackFilter
{
    private final SavedResourceCollection<Track> src;

    public SavedTrackFilter(Library library, SavedResourceCollection<Track> src)
    {
        super(library);
        this.src = src;
    }

    @Override
    void remove(List<Change<Track>> removals)
    {
        enqueue(new UnsaveTracks(src, removals.stream().map(Change::getOldIdx).toList()));
    }

    @Override
    void add(List<Change<Track>> additions)
    {
        enqueue(new SaveTracks(src, additions.stream().map(Change::getResource).toList()));
    }

    @Override
    boolean supportsMove()
    {
        return false;
    }

    @Override
    void move(List<Change<Track>> range)
    {
    }

    @Override
    public SavedResourceCollection<Track> getTarget()
    {
        return src;
    }
}
