package io.github.thomashuss.spat.client;

public class SpotifyAPIResponseException
        extends SpotifyClientException
{
    SpotifyAPIResponseException(Exception e)
    {
        super(e);
    }
}
