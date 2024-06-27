package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICacheKey;
import redis.clients.commons.csc.model.IEvictionPolicy;

import java.util.*;

public class LRUEviction implements IEvictionPolicy {

    /**
     * The cache that is associated to that policy instance
     */
    private ICache cache;
    private final Map<ICacheKey, Long> accessTimes;

    /**
     *  Constructor that gets the cache passed
     *
     * @param cache
     */
    public LRUEviction(ICache cache) {
        this();
        this.cache = cache;
    }

    /**
     * Default constructor
     */
    public LRUEviction() {
        this.accessTimes = new HashMap<>();
    }

    @Override
    public ICache getCache() {
       return this.cache;
    }

    @Override
    public void setCache(ICache cache) {
        this.cache = cache;
    }

    @Override
    public EvictionType getType() {
        return EvictionType.AGE;
    }

    @Override
    public String getName() {
        return "Simple L(east) R(ecently) U(sed)";
    }

    @Override
    public ICacheKey evictNext() {

        int currSize = this.cache.getSize();
        ICacheKey toBeEvicted = null;

        //There might be cases that a cache key that we want to evict is already evicted. So let's ensure that the eviction
        //impacts the cache
        while (this.cache.getSize() > currSize-1) {
            toBeEvicted = Collections.min(this.accessTimes.entrySet(), Map.Entry.comparingByValue()).getKey();

            //Deletion includes resetting the policy entry
            this.cache.delete(toBeEvicted);
            //this.reset(toBeEvicted);
        }

        return toBeEvicted;
    }

    @Override
    public List<ICacheKey> evictMany(int n) {
        List<ICacheKey> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            result.add(this.evictNext());
        }

        return result;
    }

    @Override
    public void touch(ICacheKey cacheKey) {
        this.accessTimes.put(cacheKey, new Date().getTime());
    }

    @Override
    public boolean reset(ICacheKey cacheKey) {
        return this.accessTimes.remove(cacheKey) != null;
    }

    @Override
    public int resetAll() {
        int result = this.accessTimes.size();
        accessTimes.clear();
        return result;
    }
}
