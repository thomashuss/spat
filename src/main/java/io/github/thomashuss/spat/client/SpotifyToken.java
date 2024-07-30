package io.github.thomashuss.spat.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Contains information about a Spotify session.
 */
public class SpotifyToken
{
    @JsonProperty("access_token")
    transient String accessToken;
    @JsonProperty("token_type")
    transient String tokenType;
    @JsonProperty("expires_in")
    transient int expiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("scope")
    transient String scope;
    Instant expires;
    String accessAuthorization;
}