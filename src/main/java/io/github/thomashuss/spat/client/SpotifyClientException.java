package io.github.thomashuss.spat.client;

public class SpotifyClientException
        extends Exception
{
    SpotifyClientException(String message)
    {
        super(message);
    }

    SpotifyClientException(Exception e)
    {
        super(e);
    }
}
