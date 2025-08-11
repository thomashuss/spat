package io.github.thomashuss.jspat.library;

import org.apache.fury.memory.MemoryBuffer;

class AlbumKV
        extends FinalizingResourceKV<Album>
{
    AlbumKV(Library library)
    {
        super(library, Album.class, "album", false);
    }

    @Override
    void serialize(MemoryBuffer buf, Album album)
    {
        Library.fury.serialize(buf, album);
        writeResourceField(buf, album.getLabel());
        writeResourceArray(buf, album.getArtists());
        writeResourceArray(buf, album.getTracks());
        writeResourceArray(buf, album.getGenres());
    }

    @Override
    Runnable finalize(Album album, MemoryBuffer buf)
    {
        final String labelKey = readResourceKeyField(buf);
        final String[] artistKeys = readResourceKeyArray(buf);
        final String[] trackKeys = readResourceKeyArray(buf);
        final String[] genreKeys = readResourceKeyArray(buf);
        return () -> {
            readResourceField(labelKey, library.labelDb::read, album::setLabel);
            album.setArtists(readResourceArray(Artist[]::new, artistKeys, library.artistDb::read));
            album.setTracks(readResourceArray(Track[]::new, trackKeys, library.trackDb::read));
            album.setGenres(readResourceArray(Genre[]::new, genreKeys, library.genreDb::read));
        };
    }
}
