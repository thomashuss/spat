package io.github.thomashuss.spat.client;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/**
 * Contains information about a Spotify session.
 */
public final class Token
{
    private Instant expires;
    private String accessAuthorization;
    private String refreshToken;

    public void setExpires(Instant expires)
    {
        this.expires = expires;
    }

    public void update(Token other)
    {
        expires = other.getExpires();
        accessAuthorization = other.getAccessAuthorization();
        refreshToken = other.getRefreshToken();
    }

    @JsonIgnore
    public boolean isValid()
    {
        return accessAuthorization != null;
    }

    public void setAccessAuthorization(String accessAuthorization)
    {
        this.accessAuthorization = accessAuthorization;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public Instant getExpires()
    {
        return expires;
    }

    public String getAccessAuthorization()
    {
        return accessAuthorization;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }
}
