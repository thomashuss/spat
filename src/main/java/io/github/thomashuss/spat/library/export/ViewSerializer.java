package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.thomashuss.spat.Spat;

import java.io.IOException;

abstract class ViewSerializer<T>
        extends JsonSerializer<T>
{
    private final ObjectWriter writer;

    ViewSerializer(Class<?> view)
    {
        writer = Spat.mapper.writerWithView(view);
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
    throws IOException
    {
        writer.writeValue(gen, value);
    }
}
