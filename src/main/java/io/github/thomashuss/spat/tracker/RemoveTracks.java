package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.client.ProgressTracker;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyClientException;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedTrack;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

public class RemoveTracks
        extends Edit
        implements TrackRemoval
{
    private final boolean isSequential;
    private final List<SavedResource<Track>> sr;
    private final Playlist playlist;
    private final List<Integer> indices;

    RemoveTracks(Playlist playlist, List<Integer> indices)
    {
        this.playlist = playlist;
        this.indices = indices;
        indices.sort(Comparator.reverseOrder());
        sr = new ArrayList<>(indices.size());
        int prev = -1;
        boolean isSequential = true;
        for (int i : indices) {
            sr.add(playlist.getSavedResourceAt(i));
            if (prev != -1) {
                isSequential = i == prev - 1;
            }
            prev = i;
        }
        this.isSequential = isSequential;
    }

    public static RemoveTracks of(Playlist playlist, List<Integer> indices)
    {
        return new RemoveTracks(playlist, indices);
    }

    RemoveTracks(Playlist playlist, int startIndex, int numEntries)
    {
        this.playlist = playlist;
        indices = Stream.iterate(startIndex + numEntries - 1, i -> i - 1)
                .limit(numEntries)
                .toList();
        isSequential = true;
        sr = indices.stream().map(playlist::getSavedResourceAt).toList();
    }

    public static RemoveTracks of(Playlist playlist, int startIndex, int numEntries)
    {
        return new RemoveTracks(playlist, startIndex, numEntries);
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
        return playlist;
    }

    @Override
    void commit(Library library)
    {
        indices.forEach(playlist::removeResource);
    }

    @Override
    void revert(Library library)
    {
        ListIterator<Integer> indIt = indices.listIterator(indices.size());
        ListIterator<SavedResource<Track>> srIt = sr.listIterator(sr.size());
        while (indIt.hasPrevious()) {
            library.saveTrackToCollection((SavedTrack) srIt.previous(), playlist, indIt.previous());
        }
    }

    @Override
    void push(SpotifyClient client, ProgressTracker progressTracker)
    throws SpotifyClientException, IOException
    {
        client.removeTracksFromPlaylist(playlist, sr.stream().map(SavedResource::getResource).toList(), progressTracker);
    }

    @Override
    public String toString()
    {
        final int size = indices.size();
        return "Remove " + (size == 1 ? sr.get(0) + " from " : size + " tracks from ") + playlist.getName();
    }
}

