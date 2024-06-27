package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICacheEntry;
import redis.clients.commons.csc.model.ICacheKey;

import java.nio.ByteBuffer;

/**
 * A cache entry that uses a cache key that is based on Strings
 *
 */
public class StrCacheEntry implements ICacheEntry {

    private final ICache cache;
    private final ByteBuffer value;
    private final StrCacheKey cacheKey;

    public StrCacheEntry(StrCacheKey cacheKey, ByteBuffer value, ICache cache) {
        this.cache = cache;
        this.cacheKey = cacheKey;
        this.value = value;
    }

    @Override
    public ICache getCache() {
        return this.cache;
    }

    @Override
    public ICacheKey getCacheKey() {
        return this.cacheKey;
    }

    @Override
    public ByteBuffer getValue() {
        return this.value;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }
}
