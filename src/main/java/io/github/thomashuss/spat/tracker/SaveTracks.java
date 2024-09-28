package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SaveTracks
        extends SrcEdit<Track>
        implements TrackInsertion
{
    private final ZonedDateTime addedAt;
    private final List<Track> tracks;
    private final int index;

    SaveTracks(SavedResourceCollection<Track> ls, List<Track> tracks)
    {
        super(ls);
        index = ls.getNumResources();
        this.tracks = new ArrayList<>(tracks.size());
        ListIterator<Track> li = tracks.listIterator(tracks.size());
        while (li.hasPrevious()) {
            this.tracks.add(li.previous());
        }
        this.addedAt = ZonedDateTime.now();
    }

    public static SaveTracks of(Library library, List<Track> tracks)
    throws IllegalEditException
    {
        SavedResourceCollection<Track> ls = library.getLikedSongs();
        if (ls.containsAnyOf(tracks))
            throw new IllegalEditException(ls, "Tracks are already saved");
        return new SaveTracks(ls, tracks);
    }

    @Override
    public int index()
    {
        return index;
    }

    @Override
    public List<Track> tracks()
    {
        return tracks;
    }

    @Override
    void commit(Library library)
    {
        library.saveResourcesToCollection(tracks, addedAt, src);
    }

    @Override
    void revert(Library library)
    {
        src.removeSavedResourcesInRange(index, index + tracks.size());
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.saveTracks(tracks, progressTracker);
    }

    @Override
    public String toString()
    {
        final int size = tracks.size();
        return "Save " + (size == 1 ? tracks.get(0)
                : size + " tracks");
    }
}
