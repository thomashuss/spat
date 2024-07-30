package io.github.thomashuss.spat.library;

import java.io.Serial;

/**
 * Defines a model for a music genre, which is required by <code>Artist</code>.  This class does not exist as part of
 * Spotify's model.
 */
public class Genre
        extends NamedResource
{
    @Serial
    private static final long serialVersionUID = 1L;

    Genre(String name)
    {
        super(name);
    }
}
