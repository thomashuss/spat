package io.github.thomashuss.jspat.library.export;

import com.fasterxml.jackson.annotation.JsonView;

@JsonView({TrackAlbumExport.Exempt.class, AlbumTrackExport.Exempt.class})
abstract class ArtistMixin
{
    @JsonView({TrackAlbumExport.class, AlbumTrackExport.class})
    private String id;
    @JsonView({TrackAlbumExport.class, AlbumTrackExport.class})
    private String name;
}
