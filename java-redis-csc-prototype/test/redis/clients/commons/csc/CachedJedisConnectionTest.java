package redis.clients.commons.csc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.commons.csc.model.ICache;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class CachedJedisConnectionTest {

    static CachedJedisConnection flushCon;
    static int CACHE_SIZE = 10000;

    @BeforeEach
    void init() {
        flushCon = new CachedJedisConnection("localhost", 6379, new SimpleCache(CACHE_SIZE, new LRUEviction()));

        //flushAll seems to cause an exception
        //flushCon.getInner().flushAll();
        flushCon.getInner().sendCommand(Protocol.Command.FLUSHALL);
    }

    @Test
    void set() {
        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);
        assertEquals("OK", conn.set("hello", "world"));
    }

    @Test
    void get() {
        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);
        assertEquals("OK", conn.set("hello", "world"));
        assertEquals("world", conn.get("hello"));
    }


    @Test
    void getCached() {
        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);
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
       ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
       CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);

        int size = conn.getCache().getSize();
        assertEquals(0, size);

        //Using 10% of the cache
        for (int i = 0; i < CACHE_SIZE/10; i++) {
            conn.set("wo-evict:" + i, ""+i);
            conn.get("wo-evict:" + i);
        }

        assertEquals(size + CACHE_SIZE/10, conn.getCache().getSize());
    }


    @Test
    void verifySocketTest() throws IOException {
        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);

        Socket socket = conn.getSocket();
        assertEquals(0, socket.getInputStream().available());
    }

    @Test
    void cacheWithEviction() {
        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);

        int testSize = (CACHE_SIZE*10)-1;
        int size = conn.getCache().getSize();
        assertEquals(0, size);

        StrCacheKey testKey = null;

        //Using 10 times the cache size
        for (int i = 0; i <= testSize; i++) {

            String keyStr = "w-evict:" + i;
            conn.set(keyStr, ""+i);
            assertEquals(conn.get(keyStr), ""+i);

            //Check if the last key is still there.
            if (i == testSize) {
                testKey = new StrCacheKey("GET", Arrays.asList(keyStr));
            }
        }

        assertEquals(cache.getMaxSize(), conn.getCache().getSize());

        //The first key should have been evicted
        assertEquals(null, cache.get(new StrCacheKey("GET", Arrays.asList("w-evict:0"))));

        //Last key should still be there
        assertEquals("99999",new String(cache.get(testKey).array(), Charset.defaultCharset()));
    }

    @Test
    void checkForInvalidationAtCacheHitTest() throws IOException, TimeoutException, InterruptedException, ParseException {

        ICache cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedJedisConnection conn = new CachedJedisConnection("localhost", 6379, cache);

        UnifiedJedis ctrl = new UnifiedJedis(new HostAndPort("localhost", 6379));

        conn.set("hello", "world");
        conn.get("hello");

        assertEquals(1, conn.getCache().getSize());

        //hasData doesn't block
        int i = 0;
        while (!conn.hasData()) {
            System.out.println("Checking for data, but don't read any ...");
            Thread.sleep(1000);
            if (i == 10)
                ctrl.set("hello", "again");
            else
                i++;
        }

        assertEquals(10, i);

        //This should come from the cache
        conn.get("hello");

        //The cache hit should have triggered that the invalidation message got processed
        assertEquals(0, conn.getCache().getSize());

        //This should go to the server
        conn.get("hello");
        assertEquals(1, conn.getCache().getSize());
    }

}