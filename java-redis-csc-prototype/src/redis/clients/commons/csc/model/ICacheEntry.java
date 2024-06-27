package redis.clients.commons.csc.model;

import java.nio.ByteBuffer;

/**
 * A cache entry is made of a cache key and the value to be cached
 */
public interface ICacheEntry {

    /**
     * @return The cache to which this entry belongs
     */
    ICache getCache();

    /**
     * @return The key under which this entry is stored
     */
    ICacheKey getCacheKey();

    /**
     * @return The value of the cache entry
     */
    ByteBuffer getValue();

    /**
     * Verifies if this entry has any value
     *
     * @return True if the cache entry could be constructed correctly, false otherwise
     */
    boolean hasValue();
}
