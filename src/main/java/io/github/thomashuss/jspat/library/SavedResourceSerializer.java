package io.github.thomashuss.jspat.library;

import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static io.github.thomashuss.jspat.library.ResourceKV.readResourceKeyField;

abstract class SavedResourceSerializer<T extends SavedResource<R>, R extends LibraryResource>
        extends Serializer<T>
{
    private final Library library;
    private final Supplier<T> constructor;
    private final ResourceKV<R> db;

    SavedResourceSerializer(Library library, Class<T> type,
                            Supplier<T> constructor, ResourceKV<R> db)
    {
        super(Library.fury, type);
        this.library = library;
        this.constructor = constructor;
        this.db = db;
    }

    Runnable finalize(T sr, MemoryBuffer buf)
    {
        final String resourceKey = readResourceKeyField(buf);
        return () -> sr.setResource(db.read(resourceKey));
    }

    @Override
    public T read(MemoryBuffer buffer)
    {
        T t = constructor.get();
        t.setAddedAt((ZonedDateTime) fury.readNonRef(buffer));
        library.needsFinalize.add(finalize(t, buffer));
        return t;
    }

    @Override
    public void write(MemoryBuffer buffer, T value)
    {
        fury.writeNonRef(buffer, value.addedAt());
        fury.writeString(buffer, value.getResource().getKey());
    }
}
