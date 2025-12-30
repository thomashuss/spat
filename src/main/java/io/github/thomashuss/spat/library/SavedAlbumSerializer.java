package io.github.thomashuss.spat.library;

class SavedAlbumSerializer
        extends SavedResourceSerializer<SavedAlbum, Album>
{
    SavedAlbumSerializer(Library library)
    {
        super(library, SavedAlbum.class, SavedAlbum::new, library.albumDb);
    }
}
