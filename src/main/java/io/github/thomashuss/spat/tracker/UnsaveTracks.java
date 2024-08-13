package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.SavedTrack;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Stream;

public class UnsaveTracks
        extends Edit
        implements TrackRemoval
{
    private final List<Integer> indices;
    private final List<SavedResource<Track>> sr;
    private final SavedResourceCollection<Track> ls;
    private final boolean isSequential;

    public UnsaveTracks(Library library, List<Integer> indices)
    {
        ls = library.getLikedSongs();
        this.indices = indices;
        indices.sort(Comparator.reverseOrder());
        boolean isSequential = true;
        sr = new ArrayList<>(indices.size());
        int prev = -1;
        for (int i : indices) {
            sr.add(ls.removeResource(i));
            if (isSequential && prev != -1) {
                isSequential = i == prev - 1;
            }
            prev = i;
        }
        this.isSequential = isSequential;
    }

    public UnsaveTracks(Library library, int startIndex, int numEntries)
    {
        indices = Stream.iterate(startIndex + numEntries - 1, i -> i - 1)
                .limit(numEntries)
                .toList();
        ls = library.getLikedSongs();
        sr = indices.stream().map(ls::removeResource).toList();
        isSequential = true;
    }

    @Override
    public List<Integer> indices()
    {
        return indices;
    }

    @Override
    public boolean isSequential()
    {
        return isSequential;
    }

    @Override
    public LibraryResource getTarget()
    {
        return Objects.requireNonNullElse(ls, null);
    }

    @Override
    void commit(Library library)
    {
        indices.forEach(ls::removeResource);
    }

    @Override
    void revert(Library library)
    {
        ListIterator<Integer> indIt = indices.listIterator(indices.size());
        ListIterator<SavedResource<Track>> srIt = sr.listIterator(sr.size());
        while (indIt.hasPrevious()) {
            library.saveTrackToCollection((SavedTrack) srIt.previous(), ls, indIt.previous());
        }
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.unsaveTracks(sr.stream().map(SavedResource::getResource).toList(), progressTracker);
    }

    @Override
    public String toString()
    {
        final int size = indices.size();
        return "Unsave " + (size == 1 ? sr == null ? "1 track" : sr.get(0) : size + " tracks");
    }
}
