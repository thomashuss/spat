package io.github.thomashuss.jspat.client;

/**
 * Thrown when the Spotify client can communicate with the server but received a non-OK response code.
 */
public class SpotifyClientHttpException
        extends SpotifyClientException
{
    private final int responseCode;

    SpotifyClientHttpException(int responseCode)
    {
        // TODO: also include message from server
        super("HTTP " + responseCode);
        this.responseCode = responseCode;
    }

    public int getResponseCode()
    {
        return responseCode;
    }
}
