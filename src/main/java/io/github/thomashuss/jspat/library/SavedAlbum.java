package io.github.thomashuss.jspat.library;

import java.time.ZonedDateTime;

public final class SavedAlbum
        extends SavedResource<Album>
{
    SavedAlbum()
    {
        super();
    }

    public SavedAlbum(ZonedDateTime addedAt, Album album)
    {
        super(addedAt, album);
    }
}
