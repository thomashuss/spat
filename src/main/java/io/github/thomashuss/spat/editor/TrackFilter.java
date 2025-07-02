package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.Track;

public abstract class TrackFilter
    extends ResourceFilter<Track>
{
    public TrackFilter(Library library)
    {
        super(library);
    }

    @Override
    Track getByKey(String key)
    {
        return library.getTrack(key);
    }
}
