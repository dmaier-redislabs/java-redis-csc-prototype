package redis.clients.commons.csc;

import redis.clients.commons.csc.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimpleCache implements ICache {

    private int maxSize;
    private IEvictionPolicy policy;
    private Map<ICacheKey, ByteBuffer> inner;
    private Map<String, List<ICacheKey>> byRedisKeyIdx;


    /**
     * A simple cache based on a Hashmap
     *
     * @param maxSize The maximum size of the cache
     * @param policy The eviction policy
     */
    public SimpleCache(int maxSize, IEvictionPolicy policy) {
            this.maxSize = maxSize;
            this.policy = policy;
            this.policy.setCache(this);
            this.byRedisKeyIdx = new HashMap<>();
            this.inner = new HashMap<>();
    }

    /**
     * @return The current size of the cache
     */
    public int getSize() {
        return this.inner.size();
    }


    @Override
    public int getMaxSize() {
        return this.maxSize;
    }

    @Override
    public Collection<ICacheEntry> getCacheEntries() {

        HashSet<ICacheEntry> result = new HashSet<>();

        for ( Map.Entry<ICacheKey, ByteBuffer> e : inner.entrySet()) {

            result.add(new StrCacheEntry((StrCacheKey) e.getKey(), e.getValue(), this));
        }

        return result;
    }

    @Override
    public ByteBuffer get(ICacheKey cacheKey) {
        this.policy.touch(cacheKey);
        return inner.get(cacheKey);
    }

    @Override
    public ICacheEntry set(ICacheKey cacheKey, ByteBuffer value) {

        //Evict entries if the cache is full
        //TODO: We might want to implement a more sophisticated logic
        if (!this.inner.containsKey(cacheKey) && this.getSize() == this.maxSize)
            this.policy.evictNext();

        //Store the entry
        ICacheEntry result = new StrCacheEntry((StrCacheKey) cacheKey, inner.put(cacheKey, value), this);

        //Update the index
        for (String k : result.getCacheKey().getRedisKeyStr()) {
            List<ICacheKey> cacheKeyList = this.byRedisKeyIdx.get(k);

            //Create the list if it doesn't exist
            if (cacheKeyList == null)
                cacheKeyList = new ArrayList<>();
                this.byRedisKeyIdx.put(k, cacheKeyList);

            cacheKeyList.add(cacheKey);
        }

        return result;
    }

    @Override
    public Boolean delete(ICacheKey cacheKey) {

        boolean result = this.inner.remove(cacheKey) != null;
        this.policy.reset(cacheKey);

        for (String k : cacheKey.getRedisKeyStr()) {
            List<ICacheKey> cacheKeyList = this.byRedisKeyIdx.get(k);
            cacheKeyList.remove(cacheKey);
            if (cacheKeyList.isEmpty())
                this.byRedisKeyIdx.remove(k);
        }

        return result;
    }

    @Override
    public List<Boolean> delete(List<ICacheKey> cacheKeys) {

        ArrayList<Boolean> result = new ArrayList<>();

        for (ICacheKey k : cacheKeys) {
            result.add(this.delete(k));
        }

        return result;
    }

    @Override
    public List<ICacheKey> deleteByRedisKeyStr(String key) {

        List<ICacheKey> cacheKeys = new ArrayList<>(this.byRedisKeyIdx.get(key));

        for (ICacheKey cacheKey : cacheKeys) {
            this.delete(cacheKey);
        }

        return cacheKeys;
    }

    @Override
    public List<ICacheKey> deleteByRedisKeyStr(List<String> keys) {

        List<ICacheKey> result = new ArrayList<>();

        for (String k : keys) {
            result.addAll(this.deleteByRedisKeyStr(k));
        }

        return result;
    }

    @Override
    public List<ICacheKey> deleteByRedisKey(ByteBuffer key) {
        String keyStr = new String(key.array(), StandardCharsets.UTF_8);
        return this.deleteByRedisKeyStr(keyStr);
    }

    @Override
    public List<ICacheKey> deleteByRedisKey(List<ByteBuffer> keys) {
        List<ICacheKey> result = new ArrayList<>();

        for (ByteBuffer k : keys) {
            result.addAll(this.deleteByRedisKey(k));
        }

        return result;
    }


    @Override
    public int flush() {

        int result = this.inner.size();

        this.inner.clear();
        this.byRedisKeyIdx.clear();
        this.policy.resetAll();

        return result;
    }

    /**
     * For now, only GET is cacheable. We indeed need to come up with a better list.
     *
     * @param cmd The Redis command
     * @return True if the command is cachable
     */
    @Override
    public Boolean isCacheable(String cmd) {
        return cmd.toUpperCase().equals("GET");
    }

    @Override
    public Boolean isCacheable(ICacheKey cacheKey) {
        return isCacheable(cacheKey.getCmd());
    }

    @Override
    public Boolean hasCacheKey(ICacheKey cacheKey) {
        return this.inner.containsKey(cacheKey);
    }

    @Override
    public IEvictionPolicy getEvictionPolicy() {
        return this.policy;
    }
}
