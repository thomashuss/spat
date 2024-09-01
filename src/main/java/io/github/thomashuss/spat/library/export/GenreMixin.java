package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.annotation.JsonView;

@JsonView(TrackAlbumExport.Exempt.class)
abstract class GenreMixin
{
    @JsonView(TrackAlbumExport.class)
    private String name;
}
