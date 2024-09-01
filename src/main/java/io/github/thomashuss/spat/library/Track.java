package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

/**
 * Corresponds to a track in Spotify's model.  Depends on <code>Artist</code> and <code>Album</code> and
 * is required by <code>SavedResourceCollection</code> and <code>Album</code>.
 */
public class Track
        extends SpotifyResource
{
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Album album;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private transient Artist[] artists;
    @JsonProperty("duration_ms")
    private int duration;
    @JsonProperty("explicit")
    private boolean explicit;
    @JsonProperty("popularity")
    private byte popularity;
    @JsonProperty("preview_url")
    private URL previewUrl;
    @JsonProperty("is_playable")
    private boolean isPlayable = true;
    private AudioFeatures features;

    Track(String id)
    {
        super(id);
    }

    public Album getAlbum()
    {
        return album;
    }

    public void setAlbum(Album album)
    {
        this.album = album;
    }

    public AudioFeatures getFeatures()
    {
        return features;
    }

    public void setFeatures(AudioFeatures features)
    {
        this.features = features;
    }

    public Artist[] getArtists()
    {
        return artists;
    }

    public void setArtists(Artist[] artists)
    {
        this.artists = artists;
    }

    public int getDuration()
    {
        return duration;
    }

    public boolean isExplicit()
    {
        return explicit;
    }

    public byte getPopularity()
    {
        return popularity;
    }

    public URL getPreviewUrl()
    {
        return previewUrl;
    }

    @JsonIgnore
    public boolean isPlayable()
    {
        return isPlayable;
    }

    @Override
    public String getResourceTypeName()
    {
        return "track";
    }
}
