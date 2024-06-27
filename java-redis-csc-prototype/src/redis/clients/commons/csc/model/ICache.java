package redis.clients.commons.csc.model;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * The cache that is used by a connection
 */
public interface ICache {


    /**
     * @return The size of the cache
     */
    int getMaxSize();

    /**
     * @return The current size of the cache
     */
    int getSize();

    /**
     * @return All the entries within the cache
     */
    Collection<ICacheEntry> getCacheEntries();

    /**
     * Fetches a value from the cache
     *
     * @param cacheKey The key within the cache
     * @return The entry within the cache
     */
    ByteBuffer get(ICacheKey cacheKey);

    /**
     * Puts a value into the cache
     *
     * @param cacheKey The key by which the value can be accessed within the cache
     * @param value The value to be put into the cache
     * @return The cache entry
     */
    ICacheEntry set(ICacheKey cacheKey, ByteBuffer value);


    /**
     * Delete an entry by cache key
     * @param cacheKey The cache key of the entry in the cache
     * @return True if the entry could be deleted, false if the entry wasn't found.
     */
    Boolean delete(ICacheKey cacheKey);


    /**
     * Delete entries by cache key from the cache
     *
     * @param cacheKeys The cache keys of the entries that should be deleted
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    List<Boolean> delete(List<ICacheKey> cacheKeys);


    /**
     * Delete an entry by the Redis key from the cache
     *
     * @param key The Redis key as binary
     * @return True if the entry could be deleted. False if the entry was not there.
     */
    List<ICacheKey> deleteByRedisKey(ByteBuffer key);

    /**
     * Delete entries by the Redis key from the cache
     *
     * @param keys The Redis keys as binaries
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    List<ICacheKey> deleteByRedisKey(List<ByteBuffer> keys);

    /**
     * Delete an entry by the Redis key from the cache
     *
     * @param key The Redis key as string
     * @return True if the entry could be deleted. False if the entry was not there.
     */
    List<ICacheKey> deleteByRedisKeyStr(String key);

    /**
     * Delete entries by the Redis key from the cache
     *
     * @param keys The Redis keys as strings
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    List<ICacheKey> deleteByRedisKeyStr(List<String> keys);

    /**
     * Flushes the entire cache
     *
     * @return Return the number of entries that were flushed
     */
    int flush();


    /**
     * @param cmd The Redis command
     * @return True if the command is cacheable, false otherwise
     */
    Boolean isCacheable(String cmd);

    /**
     * @param cacheKey The key of the cache entry
     * @return True if the entry is cachable, false otherwise
     */
    Boolean isCacheable(ICacheKey cacheKey);

    /**
     *
     * @param cacheKey The key of the cache entry
     * @return True if the cache already contains the key
     */
    Boolean hasCacheKey(ICacheKey cacheKey);


    /**
     * @return The eviction policy that is used by the cache
     */
    IEvictionPolicy getEvictionPolicy();
}
