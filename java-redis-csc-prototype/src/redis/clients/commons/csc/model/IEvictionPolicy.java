package redis.clients.commons.csc.model;

import java.util.List;

/**
 * Describes the properties and functionality of an eviction policy
 * <p>
 * One policy instance belongs to exactly one cache instance
 */
public interface IEvictionPolicy {

    /**
     * Types of eviction policies
     *
     * AGE - based on the time of access, e.g., LRU
     * FREQ - based on the frequency of access, e.g., LFU
     * HYBR - AGE + FREQ, e.g., CLOCK
     * MISC - Anythin that isn't time based, frequency based or a combination of the two, e.g., FIFO
     */
    enum EvictionType { AGE, FREQ, HYBR, MISC }

    /**
     * @return The cache that is associated to this policy instance
     */
    ICache getCache();

    /**
     * Sets the cache of this eviction, should only be called once.
     */
    void setCache(ICache cache);

    /**
     * @return The type of policy
     */
    EvictionType getType();

    /**
     * @return The name of the policy
     */
    String getName();

    /**
     * Evict the next element from the cache
     * @return The key of the entry that was evicted
     */
    ICacheKey evictNext();

    /**
     *
     * @param n The number of entries to evict
     * @return The list of keys of evicted entries
     */
    List<ICacheKey> evictMany(int n);

    /**
     * Indicates that a cache key was touched
     * @param cacheKey The key within the cache
     */
    void touch(ICacheKey cacheKey);

    /**
     * Resets the state that the eviction policy maintains about the cache key
     * @param cacheKey
     */
    boolean reset(ICacheKey cacheKey);


    /**
     * Resets the entire state of the eviction data
     * @return True if the reset could be performed successfully
     */
    int resetAll();
}
