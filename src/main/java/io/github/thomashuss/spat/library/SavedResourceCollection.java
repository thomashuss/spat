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
public class SavedResourceCollection<T extends AbstractSpotifyResource>
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

    public void addResource(SavedResource<T> r)
    {
        resources.add(r);
    }

    public void addResourceAt(SavedResource<T> r, int i)
    {
        resources.add(i, r);
    }

    public void addResources(List<SavedResource<T>> r)
    {
        resources.addAll(r);
    }

    public void addResourcesAt(List<SavedResource<T>> r, int i)
    {
        resources.addAll(i, r);
    }

    public SavedResource<T> removeResource(int index)
    {
        return resources.remove(index);
    }

    public void removeSavedResourcesInRange(int start, int end)
    {
        resources.subList(start, end).clear();
    }

    public List<T> getRange(int start, int end)
    {
        return resources.subList(start, end).stream().map(SavedResource::getResource).toList();
    }

    public void move(int insertBefore, int rangeStart, int rangeLength, boolean moveForward)
    {
        if (insertBefore > rangeStart) {
            Collections.rotate(resources.subList(rangeStart, insertBefore),
                    moveForward ? -rangeLength : rangeLength);
        } else {
            Collections.rotate(resources.subList(insertBefore, rangeStart + rangeLength),
                    moveForward ? rangeLength : -rangeLength);
        }
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

    public void reverse()
    {
        Collections.reverse(resources);
    }
}
