package io.github.thomashuss.spat.library;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

class ResourceCacheNode
        extends WeakReference<LibraryResource>
{
    private final String key;
    private final Consumer<String> onEvict;

    ResourceCacheNode(String key, LibraryResource val, ReferenceQueue<LibraryResource> rq,
                      Consumer<String> onEvict)
    {
        super(val, rq);
        this.key = key;
        this.onEvict = onEvict;
    }

    void evict()
    {
        onEvict.accept(key);
    }
}
