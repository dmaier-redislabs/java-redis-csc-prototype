package redis.clients.commons.csc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.commons.csc.model.ICache;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CachedJedisConnectionTest {

    static CachedJedisConnection conn;
    static ICache cache;
    static int CACHE_SIZE = 10000;

    @BeforeEach
    void init() {
        cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        conn = new CachedJedisConnection("localhost", 6379, cache);
        conn.getInner().flushAll();
    }
/*
    @Test
    void set() {
        assertEquals("OK", conn.set("hello", "world"));
    }

    @Test
    void get() {
        assertEquals("OK", conn.set("hello", "world"));
        assertEquals("world", conn.get("hello"));
    }


    @Test
    void getCached() {
        assertEquals("OK", conn.set("hello:1", "world"));
        int size = conn.getCache().getSize();

        //Ensure that we get only one cache entry instead of two when caching
        // the same cache key twice
        assertEquals("world", conn.get("hello:1"));
        assertEquals(size +1, conn.getCache().getSize());
        assertEquals("world", conn.get("hello:1"));
        assertEquals(size +1, conn.getCache().getSize());
    }

   @Test
    void cacheWithoutEviction() {

        int size = conn.getCache().getSize();
        assertEquals(0, size);

        //Using 10% of the cache
        for (int i = 0; i < CACHE_SIZE/10; i++) {
            conn.set("wo-evict:" + i, ""+i);
            conn.get("wo-evict:" + i);
        }

        assertEquals(size + CACHE_SIZE/10, conn.getCache().getSize());
    }
*/
    @Test
    void cacheWithEviction() {
        int testSize = (CACHE_SIZE*10)-1;
        int size = conn.getCache().getSize();
        assertEquals(0, size);

        //Using 10 times the cache size
        for (int i = 0; i <= testSize; i++) {
            conn.set("w-evict:" + i, ""+i);
            conn.get("w-evict:" + i);
        }

        assertEquals(cache.getMaxSize(), conn.getCache().getSize());
        System.out.println(cache.get(new StrCacheKey("GET", Arrays.asList("w-evict:"+testSize))).getInt());
    }

}