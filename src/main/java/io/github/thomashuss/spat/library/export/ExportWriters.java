package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.Album;
import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.Genre;
import io.github.thomashuss.spat.library.SavedAlbum;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.SavedTrack;
import io.github.thomashuss.spat.library.Track;

import java.io.IOException;
import java.io.Writer;

public class ExportWriters
{
    static {
        Spat.csvMapper.addMixIn(Album.class, AlbumCsvMixin.class);
        Spat.csvMapper.addMixIn(Artist.class, ArtistCsvMixin.class);
        Spat.csvMapper.addMixIn(Track.class, TrackCsvMixin.class);
        Spat.mapper.addMixIn(Album.class, AlbumMixin.class);
        Spat.mapper.addMixIn(Artist.class, ArtistMixin.class);
        Spat.mapper.addMixIn(Genre.class, GenreMixin.class);
        Spat.mapper.addMixIn(Track.class, TrackMixin.class);
    }

    public static final ObjectWriter jsonWriter = Spat.mapper.writer();
    public static final ObjectWriter savedTrackCsvWriter = Spat.csvMapper.writer(
            Spat.csvMapper.schemaFor(SavedTrack.class).withHeader());
    public static final ObjectWriter savedAlbumCsvWriter = Spat.csvMapper.writer(
            Spat.csvMapper.schemaFor(SavedAlbum.class).withHeader());

    /**
     * Convenience method for writing a saved resource collection.  Closes <code>writer</code>
     * when all resources have been written.
     *
     * @param src          collection to write
     * @param objectWriter Jackson writer to use
     * @param writer       stream writer to use
     * @param <T>          type of saved resource
     * @throws IOException on I/O errors
     */
    public static <T extends AbstractSpotifyResource> void writeAll(SavedResourceCollection<T> src,
                                                                    ObjectWriter objectWriter, Writer writer)
    throws IOException
    {
        SequenceWriter w = objectWriter.writeValues(writer);
        for (SavedResource<T> sr : src.getSavedResources()) {
            w.write(sr);
        }
        writer.flush();
        writer.close();
    }
}
