package io.github.thomashuss.jspat.library;

import io.github.thomashuss.jspat.editor.ResourceFilter;

import java.time.ZonedDateTime;

public class SavedAlbumCollection
        extends SavedResourceCollection<Album>
{
    SavedAlbumCollection()
    {
        super();
    }

    SavedAlbumCollection(String name)
    {
        super(name);
    }

    @Override
    SavedResource<Album> getSr(ZonedDateTime addedAt, Album album)
    {
        return new SavedAlbum(addedAt, album);
    }

    @Override
    public ResourceFilter<Album> getResourceFilter(Library library)
    {
        throw new UnsupportedOperationException();
    }
}
