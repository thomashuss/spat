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
import java.util.List;

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

    public static <T extends SavedResource<?>> ObjectWriter getWriterFor(Class<T> cls,
                                                                         boolean isJson)
    {
        if (isJson) return jsonWriter;
        else if (cls == SavedTrack.class) return savedTrackCsvWriter;
        else if (cls == SavedAlbum.class) return savedAlbumCsvWriter;
        else throw new IllegalArgumentException(cls.toString());
    }

    /**
     * Convenience method for writing multiple saved resource collections.  Closes <code>writer</code>
     * when all resources have been written.
     *
     * @param l      collections to write
     * @param isJson whether to write JSON
     * @param writer writer to write to, then close
     * @param <T>    type of saved resource
     * @throws IOException on I/O errors
     */
    public static <T extends AbstractSpotifyResource> void writeAll(List<SavedResourceCollection<T>> l,
                                                                    boolean isJson, Writer writer)
    throws IOException
    {
        SequenceWriter seqWriter = null;
        for (SavedResourceCollection<T> src : l) {
            for (SavedResource<T> sr : src.getSavedResources()) {
                if (seqWriter == null) seqWriter = getWriterFor(sr.getClass(), isJson).writeValues(writer);
                seqWriter.write(sr);
            }
        }
        writer.flush();
        if (seqWriter != null) seqWriter.close();
        writer.close();
    }
}
