package io.github.thomashuss.spat.library;

/**
 * A resource in this library which does not correspond directly to a resource in Spotify's model and is therefore
 * identified by its name.
 */
public abstract class NamedResource
        implements Comparable<String>, LibraryResource
{
    private final String name;

    NamedResource(String name)
    {
        super();
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    @Override
    public String getKey()
    {
        return name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public boolean equals(Object other)
    {
        return other instanceof NamedResource && name.equals(((NamedResource) other).getName());
    }

    @Override
    public int compareTo(String s)
    {
        return name.compareTo(s);
    }
}
