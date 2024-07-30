package io.github.thomashuss.spat.library;

import java.time.ZonedDateTime;

public final class SavedAlbum
        extends SavedResource<Album>
{
    SavedAlbum()
    {
        super();
    }

    SavedAlbum(ZonedDateTime addedAt, Album album)
    {
        super(addedAt, album);
    }
}
