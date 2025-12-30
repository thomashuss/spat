package io.github.thomashuss.spat.library.export;

import io.github.thomashuss.spat.library.Album;

/**
 * View of an album referenced by a track.
 */
class TrackAlbumExport
{
    /**
     * Do not include in export.
     */
    static class Exempt
    {
    }

    static class Ser
            extends ViewSerializer<Album>
    {
        Ser()
        {
            super(TrackAlbumExport.class);
        }
    }
}
