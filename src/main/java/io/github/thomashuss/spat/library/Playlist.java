package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Corresponds to a playlist in Spotify's model.  Depends on <code>Track</code>.
 */
public class Playlist
        extends SavedResourceCollection<Track>
        implements AbstractSpotifyResource
{
    @JsonIgnore
    private final String id;
    @JsonIgnore
    private String snapshotId;

    Playlist(String id)
    {
        super();
        this.id = id;
    }

    @Override
    @JsonIgnore
    public String getKey()
    {
        return id;
    }

    @Override
    public String getId()
    {
        return id;
    }

    public String getSnapshotId()
    {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId)
    {
        this.snapshotId = snapshotId;
    }

    @Override
    public String getResourceTypeName()
    {
        return "playlist";
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public boolean equals(Object other)
    {
        return other instanceof Playlist && id.equals(((Playlist) other).getId());
    }
}
