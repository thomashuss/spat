package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.thomashuss.spat.library.Album;

abstract class TrackMixin
{
    @JsonSerialize(using = TrackAlbumExport.Ser.class)
    @JsonView({TrackAlbumExport.class, AlbumTrackExport.Exempt.class})
    private transient Album album;
}
