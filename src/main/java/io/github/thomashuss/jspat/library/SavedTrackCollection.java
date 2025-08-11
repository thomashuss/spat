package io.github.thomashuss.jspat.library;

import io.github.thomashuss.jspat.editor.ResourceFilter;
import io.github.thomashuss.jspat.editor.SavedTrackFilter;

import java.time.ZonedDateTime;

public class SavedTrackCollection
        extends SavedResourceCollection<Track>
{
    SavedTrackCollection()
    {
        super();
    }

    SavedTrackCollection(String name)
    {
        super(name);
    }

    @Override
    SavedResource<Track> getSr(ZonedDateTime addedAt, Track track)
    {
        return new SavedTrack(addedAt, track);
    }

    @Override
    public ResourceFilter<Track> getResourceFilter(Library library)
    {
        return new SavedTrackFilter(library, this);
    }
}
