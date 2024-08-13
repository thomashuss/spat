package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.LibraryResource;

public class IllegalEditException
        extends Exception
{
    private final String reason;

    public IllegalEditException(LibraryResource target, String reason)
    {
        super("Could not edit " + target.getKey() + " for the following reason: " + reason);
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }
}
