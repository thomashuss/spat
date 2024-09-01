package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.thomashuss.spat.library.Genre;

import java.net.URL;

abstract class ArtistCsvMixin
{
    @JsonSerialize(using = StringArrSer.class)
    private transient Genre[] genres;
    @JsonSerialize(using = StringArrSer.class)
    private URL[] images;
}
