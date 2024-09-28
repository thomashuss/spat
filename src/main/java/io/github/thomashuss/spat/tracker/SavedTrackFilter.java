package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.util.List;

public class SavedTrackFilter
    extends ResourceFilter<Track>
{
    private final SavedResourceCollection<Track> src;

    public SavedTrackFilter(Library library, EditTracker tracker, SavedResourceCollection<Track> src)
    {
        super(library, tracker);
        this.src = src;
    }

    @Override
    Track getByKey(String key)
    {
        return library.getTrack(key);
    }

    @Override
    void remove(List<Change<Track>> removals, boolean isSequential)
    {
        tracker.commit(new UnsaveTracks(src, removals.stream().map(Change::getOldIdx).toList(), isSequential));
    }

    @Override
    void add(List<Change<Track>> additions)
    {
        tracker.commit(new SaveTracks(src, additions.stream().map(Change::getTarget).toList()));
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
    SavedResourceCollection<Track> getTarget()
    {
        return src;
    }
}
