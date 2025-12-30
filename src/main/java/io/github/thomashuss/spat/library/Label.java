package io.github.thomashuss.spat.library;

/**
 * Defines a model for a music label, which is required by <code>Album</code>.  This class does not exist as part of
 * Spotify's model.
 */
public class Label
        extends NamedResource
{
    Label(String name)
    {
        super(name);
    }
}
