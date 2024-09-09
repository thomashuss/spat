package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

public class UnsaveTracks
        extends Edit
        implements TrackRemoval
{
    private final List<Integer> indices;
    private final List<SavedResource<Track>> sr;
    private final SavedResourceCollection<Track> ls;
    private final boolean isSequential;

    UnsaveTracks(SavedResourceCollection<Track> ls, List<Integer> indices)
    {
        this.ls = ls;
        this.indices = indices;
        indices.sort(null);
        boolean isSequential = true;
        sr = new ArrayList<>(indices.size());
        int prev = -1;
        for (int i : indices) {
            sr.add(ls.getSavedResourceAt(i));
            if (isSequential && prev != -1) {
                isSequential = i == prev + 1;
            }
            prev = i;
        }
        this.isSequential = isSequential;
    }

    UnsaveTracks(SavedResourceCollection<Track> ls, List<Integer> indices, boolean isSequential)
    {
        this.ls = ls;
        this.indices = indices;
        sr = indices.stream().map(ls::getSavedResourceAt).toList();
        this.isSequential = isSequential;
    }

    public static UnsaveTracks of(Library library, List<Integer> indices)
    {
        return new UnsaveTracks(library.getLikedSongs(), indices);
    }

    UnsaveTracks(SavedResourceCollection<Track> ls, int startIndex, int numEntries)
    {
        indices = Stream.iterate(startIndex, i -> i + 1)
                .limit(numEntries)
                .toList();
        this.ls = ls;
        sr = indices.stream().map(ls::getSavedResourceAt).toList();
        isSequential = true;
    }

    public static UnsaveTracks of(Library library, int startIndex, int numEntries)
    {
        return new UnsaveTracks(library.getLikedSongs(), startIndex, numEntries);
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
        return ls;
    }

    @Override
    void commit(Library library)
    {
        ListIterator<Integer> indIt = indices.listIterator(indices.size());
        while (indIt.hasPrevious()) {
            ls.removeResource(indIt.previous());
        }
    }

    @Override
    void revert(Library library)
    {
        ListIterator<Integer> indIt = indices.listIterator();
        ListIterator<SavedResource<Track>> srIt = sr.listIterator();
        while (indIt.hasNext()) {
            library.saveResourceToCollection(srIt.next(), ls, indIt.next());
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
        return "Unsave " + (size == 1 ? sr.get(0) : size + " tracks");
    }
}
