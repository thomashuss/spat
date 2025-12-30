package io.github.thomashuss.spat.library;

/**
 * Defines a model for a music genre, which is required by <code>Artist</code>.  This class does not exist as part of
 * Spotify's model.
 */
public class Genre
        extends NamedResource
{
    Genre(String name)
    {
        super(name);
    }
}
