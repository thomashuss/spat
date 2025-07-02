package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.editor.ResourceFilter;

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
    public ResourceFilter<Album> getResourceFilter(Library library)
    {
        throw new UnsupportedOperationException();
    }
}
