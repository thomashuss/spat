package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.net.URL;

/**
 * Defines a musical artist in Spotify's model, which depends on <code>Genre</code> and is required by <code>Track</code>.
 */
public class Artist
        extends SpotifyResource
{
    @Serial
    private static final long serialVersionUID = 1L;
    @JsonIgnore
    private transient Genre[] genres;
    @JsonProperty("popularity")
    private byte popularity;
    @JsonIgnore
    private int followers;
    @JsonIgnore
    private URL[] images;

    Artist(String id)
    {
        super(id);
    }

    public Genre[] getGenres()
    {
        return genres;
    }

    public void setGenres(Genre[] genres)
    {
        this.genres = genres;
    }

    public byte getPopularity()
    {
        return popularity;
    }

    public int getFollowers()
    {
        return followers;
    }

    public void setFollowers(int followers)
    {
        this.followers = followers;
    }

    public URL[] getImages()
    {
        return images;
    }

    public void setImages(URL[] images)
    {
        this.images = images;
    }

    @Override
    public String getResourceTypeName()
    {
        return "artist";
    }
}
