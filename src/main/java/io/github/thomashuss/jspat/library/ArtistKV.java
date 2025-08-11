package io.github.thomashuss.jspat.library;

import org.apache.fury.memory.MemoryBuffer;

class ArtistKV
        extends FinalizingResourceKV<Artist>
{
    ArtistKV(Library library)
    {
        super(library, Artist.class, "artist", false);
    }

    @Override
    Runnable finalize(Artist artist, MemoryBuffer buf)
    {
        final String[] genreKeys = readResourceKeyArray(buf);
        return () -> artist.setGenres(readResourceArray(Genre[]::new, genreKeys, library.genreDb::read));
    }

    @Override
    void serialize(MemoryBuffer buf, Artist artist)
    {
        Library.fury.serialize(buf, artist);
        writeResourceArray(buf, artist.getGenres());
    }
}
