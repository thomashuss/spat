package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.thomashuss.spat.library.Album;
import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.AudioFeatures;

abstract class TrackCsvMixin
{
    @JsonUnwrapped(prefix = "album_")
    private transient Album album;
    @JsonSerialize(using = StringArrSer.class)
    private transient Artist[] artists;
    @JsonUnwrapped(prefix = "feature_")
    private AudioFeatures features;
}
