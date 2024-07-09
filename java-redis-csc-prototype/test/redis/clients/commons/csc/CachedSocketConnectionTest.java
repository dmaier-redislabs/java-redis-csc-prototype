package redis.clients.commons.csc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.commons.csc.model.ICache;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.TimeoutException;

class CachedSocketConnectionTest {

    static final long CMD_TIMEOUT = 2000;
    static ICache cache;
    static ICache ctrl_cache;
    static int CACHE_SIZE = 10000;

    @BeforeEach
    void init() throws IOException, TimeoutException, InterruptedException {
        cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        ctrl_cache = new SimpleCache(CACHE_SIZE, new LRUEviction());
        CachedSocketConnection flush = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, ctrl_cache);
        flush.execRawCmdStr(new String[]{"FLUSHALL"});
    }

    @Test
    void execRawCmdStrTest() throws IOException, TimeoutException {

        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, cache);

        String result = con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        assertEquals("+OK\r\n", result);

        result = con.execRawCmdStr(new String[]{"GET", "hello"});
        assertEquals("$5\r\nworld\r\n", result);

    }

    @Test
    void execRawCmdStrWithInvalidationTest() throws IOException, TimeoutException, ParseException {
        CachedSocketConnection ctrl = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, ctrl_cache);
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, cache);

        //con.execRawCmdStr(new String[]{"HELLO", "3"});
        //con.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        con.execRawCmdStr(new String[]{"GET", "hello"});

        //This should trigger Redis to send an invalidation message
        String status = ctrl.execRawCmdStr(new String[]{"SET", "hello", "again"});
        assertEquals("+OK\r\n", status);

        ByteBuffer result = con.readDataBlocking();

        InvalidationNotification notification = new InvalidationNotification(result);
        assertEquals(1, notification.getKeys().size());
        assertEquals("hello", new String(notification.getKeys().get(0).array(), Charset.defaultCharset()));
    }

    @Test
    void checkForInvalidationsTest() throws IOException, TimeoutException, InterruptedException, ParseException {
        CachedSocketConnection ctrl = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, ctrl_cache);
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, cache);

        con.execRawCmdStr(new String[]{"HELLO", "3"});
        con.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        con.execRawCmdStr(new String[]{"GET", "hello"});
        ctrl.execRawCmdStr(new String[]{"DEL", "hello"});

        //hasData doesn't block
        while (!con.hasData()) {
            System.out.println("Checking for data ...");
        }

        //readDataBlockingBytes blocks
        ByteBuffer result = con.readDataBlocking();
        InvalidationNotification notification = new InvalidationNotification(result);
        assertEquals("hello", new String(notification.getKeys().get(0).array(), Charset.defaultCharset()));

    }


    @Test
    void checkForInvalidationAtCacheHitTest() throws IOException, TimeoutException, InterruptedException, ParseException {
        CachedSocketConnection ctrl = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, ctrl_cache);
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379, CMD_TIMEOUT, cache);;

        con.execRawCmdStr(new String[]{"HELLO", "3"});
        con.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        con.execRawCmdStr(new String[]{"GET", "hello"});

        assertEquals(1, con.getCache().getSize());

        //hasData doesn't block
        int i = 0;
        while (!con.hasData()) {
            System.out.println("Checking for data, but don't read any ...");
            Thread.sleep(1000);
            if (i == 10)
                ctrl.execRawCmdStr(new String[]{"SET", "hello", "again"});
            else
                i++;
        }

        assertEquals(10, i);

        //This should come from the cache
        con.execRawCmdStr(new String[]{"GET", "hello"});

        //The cache hit should have triggered that the invalidation message got processed
        assertEquals(0, con.getCache().getSize());

        //This should go to the server
        con.execRawCmdStr(new String[]{"GET", "hello"});
        assertEquals(1, con.getCache().getSize());
    }

}