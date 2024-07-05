package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICacheKey;
import redis.clients.commons.csc.model.ICachedConnection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * Experimental wrapper class
 *
 * //TODO: The Jedis connection doesn't expose a method to consume the invalidation notifications.
 */
public class CachedJedisConnection implements ICachedConnection<UnifiedJedis> {

    /**
     * The Jedis connection
     */
    private UnifiedJedis inner;
    private ICache cache;


    /**
     * Cto
     *
     * @param host
     * @param port
     * @param cache
     */
    public CachedJedisConnection(String host, int port, ICache cache) {
        this.cache = cache;
        this.inner = new UnifiedJedis(new HostAndPort(host, port));
    }

    @Override
    public UnifiedJedis getInner() {
        return this.inner;
    }

    @Override
    public ICache getCache() {
        return this.cache;
    }

    /**
     * Wraps the set command
     * @param key
     * @param value
     * @return
     */
    String set(String key, String value) {
        return this.inner.set(key, value);
    }

    /**
     * Wraps the get command
     *
     * @param key
     * @return
     */
    String get(String key) {

        ICacheKey cacheKey = new StrCacheKey("GET", Arrays.asList(key));
        ByteBuffer value = this.cache.get(cacheKey);

        if (value != null) {
            return new String(value.array(), StandardCharsets.UTF_8);
        } else {
            String valueStr = inner.get(key);
            this.cache.set(cacheKey, ByteBuffer.wrap(valueStr.getBytes(StandardCharsets.UTF_8)));
            return valueStr;
        }
    }
}
