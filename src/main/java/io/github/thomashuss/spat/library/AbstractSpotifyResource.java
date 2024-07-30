package io.github.thomashuss.spat.library;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A class with an equivalent in Spotify's model.
 */
public interface AbstractSpotifyResource
        extends LibraryResource
{
    String getId();

    String getResourceTypeName();

    @Override
    default String getKey()
    {
        return getId();
    }

    default URI getOpenUri()
    {
        try {
            return new URI("spotify:" + getResourceTypeName() + ':' + getId());
        } catch (URISyntaxException ignored) {
        }
        return null;
    }

    default URI getWebUri()
    {
        try {
            return new URI("https://open.spotify.com/" + getResourceTypeName() + '/' + getId());
        } catch (URISyntaxException ignored) {
        }
        return null;
    }
}
