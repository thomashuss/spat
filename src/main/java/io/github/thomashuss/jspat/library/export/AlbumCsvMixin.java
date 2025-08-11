package io.github.thomashuss.jspat.library.export;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.thomashuss.jspat.library.Artist;
import io.github.thomashuss.jspat.library.Genre;
import io.github.thomashuss.jspat.library.Label;
import io.github.thomashuss.jspat.library.Track;

import java.net.URL;

abstract class AlbumCsvMixin
{
    @JsonUnwrapped(prefix = "label_")
    private transient Label label;
    @JsonSerialize(using = StringArrSer.class)
    private transient Artist[] artists;
    @JsonSerialize(using = StringArrSer.class)
    private transient Track[] tracks;
    @JsonSerialize(using = StringArrSer.class)
    private transient Genre[] genres;
    @JsonSerialize(using = StringArrSer.class)
    private URL[] images;
}
