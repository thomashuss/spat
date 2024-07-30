package io.github.thomashuss.spat.client;

/**
 * Thrown when the Spotify client can communicate with the server but received a non-OK response code.
 */
public class SpotifyClientHttpException
        extends Exception
{
    private final int responseCode;

    public SpotifyClientHttpException(int responseCode)
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
