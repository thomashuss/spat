package io.github.thomashuss.jspat.library.export;

import com.fasterxml.jackson.annotation.JsonView;

@JsonView(TrackAlbumExport.Exempt.class)
abstract class GenreMixin
{
    @JsonView(TrackAlbumExport.class)
    private String name;
}
