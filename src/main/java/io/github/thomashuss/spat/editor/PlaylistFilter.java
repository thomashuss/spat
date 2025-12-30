package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.util.List;

public class PlaylistFilter
        extends TrackFilter
{
    private final Playlist playlist;

    public PlaylistFilter(Library library, Playlist playlist)
    {
        super(library);
        this.playlist = playlist;
    }

    @Override
    void remove(List<Change<Track>> removals)
    {
        enqueue(new RemoveTracks(playlist,
                removals.stream().map(Change::getOldIdx).toList(),
                removals.stream().map(Change::getSavedResource).toList()
        ));
    }

    private void addRange(List<Change<Track>> range)
    {
        enqueue(new AddTracks(playlist, range.stream().map(Change::getResource).toList(),
                range.get(0).newIdx));
    }

    @Override
    void add(List<Change<Track>> additions)
    {
        new SequentialNewIterator<>(additions).forEachRemaining(this::addRange);
    }

    @Override
    boolean supportsMove()
    {
        return true;
    }

    @Override
    void move(List<Change<Track>> range)
    {
        Change<Track> first = range.get(range.size() - 1);
        int oldIdx = first.oldIdx;
        int newIdx = first.newIdx;
        if (oldIdx != newIdx) {
            enqueue(new MoveTracks(playlist, newIdx, oldIdx, range.size()));
        }
    }

    @Override
    public SavedResourceCollection<Track> getTarget()
    {
        return playlist;
    }
}
