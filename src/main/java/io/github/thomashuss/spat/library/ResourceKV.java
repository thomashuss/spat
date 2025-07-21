package io.github.thomashuss.spat.library;

import org.apache.fury.memory.MemoryBuffer;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Txn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class ResourceKV<T extends LibraryResource>
        implements AutoCloseable
{
    final Dbi<ByteBuffer> db;
    final Library library;
    final Class<T> valueClass;
    private final boolean shouldCommitOnInstantiation;
    private final Map<String, ResourceCacheNode> cache;

    ResourceKV(Library library, Class<T> valueClass,
               String dbKey,
               boolean shouldCommitOnInstantiation)
    {
        this.library = library;
        this.valueClass = valueClass;
        this.shouldCommitOnInstantiation = shouldCommitOnInstantiation;
        synchronized (library.env) {
            this.db = library.env.openDbi(dbKey, DbiFlags.MDB_CREATE);
        }
        cache = new HashMap<>();
    }


    static void writeResourceField(MemoryBuffer buffer, LibraryResource field)
    {
        Library.fury.writeString(buffer, field == null ? "" : field.getKey());
    }

    static void writeResourceArray(MemoryBuffer buffer, LibraryResource[] resources)
    {
        if (resources == null) {
            buffer.writeInt32(0);
        } else {
            int len = resources.length;
            buffer.writeInt32(len);
            if (len > 0) {
                for (LibraryResource resource : resources) {
                    Library.fury.writeString(buffer, resource.getKey());
                }
            }
        }
    }

    static String readResourceKeyField(MemoryBuffer buffer)
    {
        String key = Library.fury.readString(buffer);
        if (key.isEmpty()) {
            return null;
        } else {
            return key;
        }
    }

    static String[] readResourceKeyArray(MemoryBuffer buffer)
    {
        int len = buffer.readInt32();
        if (len > 0) {
            String[] ret = new String[len];
            for (int i = 0; i < len; i++) {
                ret[i] = Library.fury.readString(buffer);
            }
            return ret;
        } else {
            return null;
        }
    }

    static <T extends LibraryResource> void readResourceField(String key,
                                                              Function<String, T> reader,
                                                              Consumer<T> setter)
    {
        if (key != null) {
            T toSet = reader.apply(key);
            setter.accept(toSet);
        }
    }

    static <F extends LibraryResource> F[] readResourceArray(Function<Integer, F[]> fieldArrayConstructor,
                                                             String[] keys,
                                                             Function<String, F> func)
    {
        if (keys != null) {
            int len = keys.length;
            F[] ret = fieldArrayConstructor.apply(len);
            for (int i = 0; i < len; i++) {
                ret[i] = func.apply(keys[i]);
            }
            return ret;
        }
        return null;
    }

    abstract void serialize(MemoryBuffer buf, T t);

    abstract T deserialize(MemoryBuffer buf);

    void save(T val)
    {
        synchronized (library.env) {
            put(library.encodeKey(val.getKey()), val);
        }
    }

    T read(String key)
    {
        synchronized (library.env) {
            LibraryResource res = tryFromCache(key);
            T ret;
            if (res == null) {
                final ByteBuffer keyBuf = library.encodeKey(key);
                ret = tryFromDB(keyBuf);
                if (ret != null) putInCache(ret);
            } else {
                ret = valueClass.cast(res);
                putInCache(ret);
            }
            return ret;
        }
    }

    T readOrCreate(String key, Function<String, T> func)
    {
        synchronized (library.env) {
            library.evictResourceCacheNodes();
            LibraryResource res = tryFromCache(key);
            T val;
            if (res == null) {
                final ByteBuffer keyBuf = library.encodeKey(key);
                res = tryFromDB(keyBuf);
                if (res == null) {
                    if (func == null) {
                        return null;
                    } else {
                        val = func.apply(key);
                        if (shouldCommitOnInstantiation) put(keyBuf, val);
                    }
                } else {
                    val = valueClass.cast(res);
                }
                putInCache(val);
            } else {
                val = valueClass.cast(res);
            }
            return val;
        }
    }

    void values(Collection<T> collection)
    {
        LibraryResource r;
        T val;
        ByteBuffer valBuf;
        Set<String> keysFound = new HashSet<>();
        synchronized (library.env) {
            library.evictResourceCacheNodes();

            synchronized (library.rq) {
                for (ResourceCacheNode cacheNode : cache.values()) {
                    r = cacheNode.get();
                    if (r != null) {
                        keysFound.add(r.getKey());
                        collection.add(valueClass.cast(r));
                    }
                }
            }

            try (Txn<ByteBuffer> txn = library.env.txnRead();
                 CursorIterable<ByteBuffer> it = db.iterate(txn)) {
                for (CursorIterable.KeyVal<ByteBuffer> c : it) {
                    if (!keysFound.contains(library.decodeKey(c.key()))) {
                        valBuf = c.val();
                        if (valBuf != null) {
                            val = deserialize(MemoryBuffer.fromByteBuffer(valBuf));
                            collection.add(val);
                            putInCache(val);
                        }
                    }
                }
            }
        }
    }

    List<T> values()
    {
        List<T> list = new ArrayList<>();
        values(list);
        return list;
    }

    void remove(String key)
    {
        synchronized (library.env) {
            cache.remove(key);
            db.delete(library.encodeKey(key));
        }
    }

    @Override
    public void close()
    {
        synchronized (library.env) {
            db.close();
        }
    }

    private void putInCache(T val)
    {
        String key = val.getKey();
        synchronized (library.rq) {
            cache.put(key, new ResourceCacheNode(key, val, library.rq, cache::remove));
        }
    }

    private LibraryResource tryFromCache(String key)
    {
        synchronized (library.rq) {
            ResourceCacheNode node = cache.get(key);
            if (node != null)
                return node.get();
            return null;
        }
    }

    private T tryFromDB(Txn<ByteBuffer> txn, ByteBuffer keyBuf)
    {
        final ByteBuffer valBuf = db.get(txn, keyBuf);
        if (valBuf == null) return null;
        return deserialize(MemoryBuffer.fromByteBuffer(valBuf));
    }

    private T tryFromDB(ByteBuffer keyBuf)
    {
        try (Txn<ByteBuffer> txn = library.env.txnRead()) {
            return tryFromDB(txn, keyBuf);
        }
    }

    private void put(ByteBuffer keyBuf, T val)
    {
        library.valBuf.clear();
        library.valMemBuf.writerIndex(0);
        serialize(library.valMemBuf, val);
        library.ensureValOffHeap();
        db.put(keyBuf, library.valBuf);
        library.maybeGrowMap();
    }
}
