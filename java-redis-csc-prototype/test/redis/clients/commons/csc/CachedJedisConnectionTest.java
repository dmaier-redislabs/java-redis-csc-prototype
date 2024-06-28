package redis.clients.commons.csc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICachedConnection;
import redis.clients.jedis.UnifiedJedis;

import static org.junit.jupiter.api.Assertions.*;

class CachedJedisConnectionTest {

    static CachedJedisConnection conn;
    static ICache cache;

    @BeforeAll
    static void init() {
        cache = new SimpleCache(10000, new LRUEviction());
        conn = new CachedJedisConnection("localhost", 6379, cache);
    }

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
        assertEquals("world", conn.get("hello:1"));
        conn.get("hello:1");
        assertEquals(size +1, conn.getCache().getSize());
        assertEquals("world", conn.get("hello:1"));
        assertEquals(size +1, conn.getCache().getSize());
    }

   @Test
    void cache1000() {

        int size = conn.getCache().getSize();

        for (int i = 0; i < 1000; i++) {
            conn.set("hello:1000:" + i, "world");
            conn.get("hello:1000:" + i);
        }

        assertEquals(size + 1000, conn.getCache().getSize());
    }

    @Test
    void eviction() {
        int size = conn.getCache().getSize();

        for (int i = 0; i < 10000; i++) {
            conn.set("hello:10000:" + i, "world");
            conn.get("hello:10000:" + i);
        }
        assertEquals(cache.getMaxSize(), conn.getCache().getSize());
    }

}