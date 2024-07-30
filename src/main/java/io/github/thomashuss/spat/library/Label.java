package io.github.thomashuss.spat.library;

import java.io.Serial;

/**
 * Defines a model for a music label, which is required by <code>Album</code>.  This class does not exist as part of
 * Spotify's model.
 */
public class Label
        extends NamedResource
{
    @Serial
    private static final long serialVersionUID = 1L;

    Label(String name)
    {
        super(name);
    }
}
