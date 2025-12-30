package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.time.temporal.Temporal;

/**
 * Corresponds to an album in Spotify's model.  Depends on <code>Artist</code>, <code>Track</code>
 * and <code>Genre</code> and is required by <code>SavedResourceCollection</code>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Album
        extends SpotifyResource
{
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Label label;
    @JsonProperty(value = "release_date", access = JsonProperty.Access.READ_ONLY)
    private Temporal releaseDate;
    @JsonProperty("popularity")
    private byte popularity;
    @JsonProperty("isrc")
    private String isrc;
    @JsonProperty("ean")
    private String ean;
    @JsonProperty("upc")
    private String upc;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Artist[] artists;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Track[] tracks;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Genre[] genres;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private URL[] images;

    Album(String id)
    {
        super(id);
    }

    public Artist[] getArtists()
    {
        return artists;
    }

    public void setArtists(Artist[] artists)
    {
        if (artists == null) return;
        this.artists = artists;
    }

    public Track[] getTracks()
    {
        return tracks;
    }

    public void setTracks(Track[] tracks)
    {
        if (tracks == null) return;
        this.tracks = tracks;
    }

    public Genre[] getGenres()
    {
        return genres;
    }

    public void setGenres(Genre[] genres)
    {
        if (genres == null) return;
        this.genres = genres;
    }

    public Label getLabel()
    {
        return label;
    }

    public void setLabel(Label label)
    {
        if (label == null) return;
        this.label = label;
    }

    public URL[] getImages()
    {
        return images;
    }

    public void setImages(URL[] images)
    {
        if (images != null)
            this.images = images;
    }

    public Temporal getReleaseDate()
    {
        return releaseDate;
    }

    public void setReleaseDate(Temporal releaseDate)
    {
        this.releaseDate = releaseDate;
    }

    public byte getPopularity()
    {
        return popularity;
    }

    /**
     * International Standard Recording Code
     *
     * @return
     */
    public String getIsrc()
    {
        return isrc;
    }

    /**
     * International Article Number
     *
     * @return
     */
    public String getEan()
    {
        return ean;
    }

    /**
     * Universal Product Code
     *
     * @return
     */
    public String getUpc()
    {
        return upc;
    }

    @Override
    public String getResourceTypeName()
    {
        return "album";
    }
}
