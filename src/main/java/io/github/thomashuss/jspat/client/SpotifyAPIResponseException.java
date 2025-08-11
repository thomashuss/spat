package io.github.thomashuss.jspat.client;

public class SpotifyAPIResponseException
        extends SpotifyClientException
{
    SpotifyAPIResponseException(Exception e)
    {
        super(e);
    }
}
