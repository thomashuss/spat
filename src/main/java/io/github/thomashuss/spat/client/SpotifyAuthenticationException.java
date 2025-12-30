package io.github.thomashuss.spat.client;

/**
 * Thrown when the Spotify client successfully communicates with the server but can't authenticate.
 */
public class SpotifyAuthenticationException
        extends SpotifyClientException
{
    SpotifyAuthenticationException(String s)
    {
        super(s);
    }
}
