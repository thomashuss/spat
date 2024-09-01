package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A resource in this library which corresponds directly to a resource in Spotify's model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SpotifyResource
        implements AbstractSpotifyResource
{
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final String id;
    @JsonProperty("name")
    private String name;

    SpotifyResource(String id)
    {
        super();
        this.id = id;
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public boolean equals(Object other)
    {
        return other instanceof SpotifyResource && id.equals(((SpotifyResource) other).getId());
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }
}
