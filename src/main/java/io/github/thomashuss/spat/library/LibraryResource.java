package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface LibraryResource
{
    default String getName()
    {
        return getKey();
    }

    @JsonIgnore
    String getKey();
}
