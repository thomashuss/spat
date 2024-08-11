package io.github.thomashuss.spat.client;

import com.fasterxml.jackson.annotation.JsonProperty;

class SpotifyToken
{
    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("token_type")
    String tokenType;
    @JsonProperty("expires_in")
    int expiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("scope")
    String scope;
}