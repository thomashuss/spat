package io.github.thomashuss.jspat.library;

import org.apache.fury.memory.MemoryBuffer;

class TrackKV
        extends FinalizingResourceKV<Track>
{
    TrackKV(Library library)
    {
        super(library, Track.class, "track", false);
    }

    @Override
    Runnable finalize(Track track, MemoryBuffer buf)
    {
        final String albumKey = readResourceKeyField(buf);
        final String[] artistKeys = readResourceKeyArray(buf);
        return () -> {
            readResourceField(albumKey, library.albumDb::read, track::setAlbum);
            track.setArtists(readResourceArray(Artist[]::new, artistKeys, library.artistDb::read));
        };
    }

    @Override
    void serialize(MemoryBuffer buf, Track track)
    {
        Library.fury.serialize(buf, track);
        writeResourceField(buf, track.getAlbum());
        writeResourceArray(buf, track.getArtists());
    }
}
