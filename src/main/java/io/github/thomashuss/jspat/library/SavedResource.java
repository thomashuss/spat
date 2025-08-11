package io.github.thomashuss.jspat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Retains metadata about a particular save of a <code>LibraryResource</code>.
 */
public abstract sealed class SavedResource<T extends LibraryResource>
        implements LibraryResource
        permits SavedAlbum, SavedTrack
{
    @JsonProperty("added_at")
    private ZonedDateTime addedAt;
    @JsonUnwrapped
    private T resource;

    SavedResource()
    {
    }

    public SavedResource(ZonedDateTime addedAt, T resource)
    {
        this.addedAt = addedAt;
        this.resource = resource;
    }

    void setAddedAt(ZonedDateTime addedAt)
    {
        this.addedAt = addedAt;
    }

    public ZonedDateTime addedAt()
    {
        return addedAt;
    }

    public T getResource()
    {
        return resource;
    }

    void setResource(T resource)
    {
        this.resource = resource;
    }

    public String toString()
    {
        return resource.toString();
    }

    public int hashCode()
    {
        return Objects.hash(addedAt, resource);
    }

    public boolean equals(Object other)
    {
        if (other instanceof SavedResource<?> s)
            return resource.equals(s.getResource()) && addedAt.equals(s.addedAt());
        return false;
    }

    @Override
    @JsonIgnore
    public String getKey()
    {
        return resource.getKey();
    }

    @Override
    @JsonIgnore
    public String getName()
    {
        return resource.getName();
    }
}
