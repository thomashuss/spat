package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.editor.ResourceFilter;
import io.github.thomashuss.spat.editor.SavedTrackFilter;

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
