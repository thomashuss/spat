package io.github.thomashuss.jspat.client;

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

    public Instant getExpires()
    {
        return expires;
    }

    public void setExpires(Instant expires)
    {
        this.expires = expires;
    }

    public String getAccessAuthorization()
    {
        return accessAuthorization;
    }

    public void setAccessAuthorization(String accessAuthorization)
    {
        this.accessAuthorization = accessAuthorization;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }
}
