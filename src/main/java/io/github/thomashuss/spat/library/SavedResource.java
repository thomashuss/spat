package io.github.thomashuss.spat.library;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Retains metadata about a particular save of a <code>LibraryResource</code>.
 */
public abstract sealed class SavedResource<T extends LibraryResource>
        implements LibraryResource
        permits SavedAlbum, SavedTrack
{
    private ZonedDateTime addedAt;
    private T resource;

    SavedResource()
    {
    }

    SavedResource(ZonedDateTime addedAt, T resource)
    {
        this.addedAt = addedAt;
        this.resource = resource;
    }

    static <T extends LibraryResource> SavedResource<T> of(ZonedDateTime addedAt, LibraryResource resource)
    {
        if (resource instanceof Track t) {
            @SuppressWarnings("unchecked")
            SavedResource<T> ret = (SavedResource<T>) new SavedTrack(addedAt, t);
            return ret;
        }
        else if (resource instanceof Album a) {
            @SuppressWarnings("unchecked")
            SavedResource<T> ret = (SavedResource<T>) new SavedAlbum(addedAt, a);
            return ret;
        }
        else throw new RuntimeException("Cannot save this resource type");
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
    public String getKey()
    {
        return resource.getKey();
    }

    @Override
    public String getName()
    {
        return resource.getName();
    }
}
