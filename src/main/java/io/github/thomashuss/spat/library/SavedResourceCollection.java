package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Any user-defined collection of <code>LibraryResource</code>s.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SavedResourceCollection<T extends SpotifyResource>
        implements LibraryResource
{
    @JsonIgnore
    transient ArrayList<SavedResource<T>> resources;
    @JsonProperty("name")
    private String name;

    /**
     * Invoked if and only if deserializing from remote.
     */
    SavedResourceCollection()
    {
        resources = new ArrayList<>();
    }

    /**
     * Invoked if deserializing from local.
     *
     * @param name name of list
     */
    SavedResourceCollection(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getKey()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public int hashCode()
    {
        return resources.hashCode();
    }

    public boolean equals(Object other)
    {
        return other instanceof SavedResourceCollection && resources.equals(((SavedResourceCollection<?>) other).getSavedResources());
    }

    public List<SavedResource<T>> getSavedResources()
    {
        return Collections.unmodifiableList(resources);
    }

    public SavedResource<T> getSavedResourceAt(int i)
    {
        return resources.get(i);
    }

    public int getNumResources()
    {
        return resources.size();
    }

    public boolean isEmpty()
    {
        return resources.isEmpty();
    }

    public void clearResources()
    {
        resources.clear();
    }

    void addResource(SavedResource<T> r)
    {
        resources.add(r);
    }

    public void removeResource(int index)
    {
        resources.remove(index);
    }

    public void removeResource(T resource)
    {
        if (!resources.isEmpty()) {
            Iterator<SavedResource<T>> it = resources.iterator();
            while (it.hasNext()) {
                if (it.next().getResource().equals(resource)) {
                    it.remove();
                    break;
                }
            }
        }
    }
}
