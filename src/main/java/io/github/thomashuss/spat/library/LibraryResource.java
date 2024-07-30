package io.github.thomashuss.spat.library;

public interface LibraryResource
{
    default String getName()
    {
        return getKey();
    }

    String getKey();
}
