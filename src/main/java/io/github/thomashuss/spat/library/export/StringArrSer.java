package io.github.thomashuss.spat.library.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Arrays;

class StringArrSer
    extends JsonSerializer<Object[]>
{
    @Override
    public void serialize(Object[] value, JsonGenerator gen, SerializerProvider serializers)
    throws IOException
    {
        gen.writeArray(Arrays.stream(value)
                .map(String::valueOf)
                .toArray(String[]::new), 0, value.length);
    }
}
