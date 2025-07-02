package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.tracker.ResourceFilter;
import io.github.thomashuss.spat.tracker.SavedTrackFilter;

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
    public ResourceFilter<Track> getResourceFilter(Library library)
    {
        return new SavedTrackFilter(library, this);
    }
}
