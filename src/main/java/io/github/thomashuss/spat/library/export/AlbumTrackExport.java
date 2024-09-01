package io.github.thomashuss.spat.library.export;

import io.github.thomashuss.spat.library.Track;

/**
 * View of tracks referenced by an album.
 */
class AlbumTrackExport
{
    /**
     * Do not include in export.
     */
    static class Exempt
    {
    }

    static class Ser
            extends ViewSerializer<Track[]>
    {
        Ser()
        {
            super(AlbumTrackExport.class);
        }
    }
}
