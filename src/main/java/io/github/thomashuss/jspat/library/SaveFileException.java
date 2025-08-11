package io.github.thomashuss.jspat.library;

/**
 * Thrown when the integrity of the database file system is violated.
 */
public class SaveFileException
        extends Exception
{
    public SaveFileException(String msg)
    {
        super(msg);
    }
}
