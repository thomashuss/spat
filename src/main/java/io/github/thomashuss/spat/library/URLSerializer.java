package io.github.thomashuss.spat.library;

import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Trivially implements URL serialization to avoid the default JDK serialization implemented by URL.
 */
public class URLSerializer
        extends Serializer<URL>
{
    public URLSerializer(Fury fury)
    {
        super(fury, URL.class);
    }

    @Override
    public URL read(MemoryBuffer buffer)
    {
        try {
            return new URI(fury.readString(buffer)).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(MemoryBuffer buffer, URL value)
    {
        fury.writeString(buffer, value.toString());
    }
}
