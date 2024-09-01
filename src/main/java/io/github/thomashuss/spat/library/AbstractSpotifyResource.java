package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A class with an equivalent in Spotify's model.
 */
public interface AbstractSpotifyResource
        extends LibraryResource
{
    String getId();

    @JsonIgnore
    String getResourceTypeName();

    @Override
    @JsonIgnore
    default String getKey()
    {
        return getId();
    }

    @JsonIgnore
    default URI getOpenUri()
    {
        try {
            return new URI("spotify:" + getResourceTypeName() + ':' + getId());
        } catch (URISyntaxException ignored) {
        }
        return null;
    }

    @JsonIgnore
    default URI getWebUri()
    {
        try {
            return new URI("https://open.spotify.com/" + getResourceTypeName() + '/' + getId());
        } catch (URISyntaxException ignored) {
        }
        return null;
    }
}
