package io.github.thomashuss.jspat.editor;

import io.github.thomashuss.jspat.library.Library;
import io.github.thomashuss.jspat.library.Track;

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
