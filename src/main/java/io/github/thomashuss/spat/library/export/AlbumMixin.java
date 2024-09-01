package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.thomashuss.spat.library.Track;

abstract class AlbumMixin
{
    @JsonView(TrackAlbumExport.Exempt.class)
    @JsonSerialize(using = AlbumTrackExport.Ser.class)
    private transient Track[] tracks;
}
