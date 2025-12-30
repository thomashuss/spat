package io.github.thomashuss.spat.library;

import java.time.ZonedDateTime;

public final class SavedTrack
        extends SavedResource<Track>
{
    SavedTrack()
    {
        super();
    }

    public SavedTrack(ZonedDateTime addedAt, Track track)
    {
        super(addedAt, track);
    }
}
