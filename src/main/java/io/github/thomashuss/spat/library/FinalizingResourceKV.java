package io.github.thomashuss.spat.library;

import org.apache.fury.memory.MemoryBuffer;

abstract class FinalizingResourceKV<T extends LibraryResource>
        extends ResourceKV<T>
{
    FinalizingResourceKV(Library library, Class<T> valueClass, String dbKey, boolean shouldCommitOnInstantiation)
    {
        super(library, valueClass, dbKey, shouldCommitOnInstantiation);
    }

    abstract Runnable finalize(T t, MemoryBuffer buf);

    @Override
    T deserialize(MemoryBuffer buf)
    {
        T ret = valueClass.cast(Library.fury.deserialize(buf));
        library.needsFinalize.add(finalize(ret, buf));
        return ret;
    }
}
