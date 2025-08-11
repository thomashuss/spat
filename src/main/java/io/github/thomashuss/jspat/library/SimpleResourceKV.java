package io.github.thomashuss.jspat.library;

import org.apache.fury.memory.MemoryBuffer;

abstract class SimpleResourceKV<T extends LibraryResource>
        extends ResourceKV<T>
{
    SimpleResourceKV(Library library, Class<T> valueClass, String dbKey, boolean shouldCommitOnInstantiation)
    {
        super(library, valueClass, dbKey, shouldCommitOnInstantiation);
    }

    @Override
    void serialize(MemoryBuffer buf, T t)
    {
        Library.fury.serialize(buf, t);
    }

    @Override
    T deserialize(MemoryBuffer buf)
    {
        return valueClass.cast(Library.fury.deserialize(buf));
    }
}
