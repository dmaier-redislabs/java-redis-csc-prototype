package redis.clients.commons.csc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.TimeoutException;

class CachedSocketConnectionTest {

    @Test
    void execRawCmdStrTest() throws IOException, TimeoutException, InterruptedException {

        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379);
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        String result = con.execRawCmdStr(new String[]{"GET", "hello"});
        assertEquals("$5\r\nworld\r\n", result);

    }

    @Test
    void execRawCmdStrWithInvalidationTest() throws IOException, TimeoutException, InterruptedException, ParseException {
        CachedSocketConnection ctrl = new CachedSocketConnection("localhost", 6379);
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379);

        con.execRawCmdStr(new String[]{"HELLO", "3"});
        con.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        con.execRawCmdStr(new String[]{"GET", "hello"});

        //This should trigger Redis to send an invalidation message
        String status = ctrl.execRawCmdStr(new String[]{"SET", "hello", "again"});
        assertEquals("+OK\r\n", status);

        ByteBuffer result = con.readDataBlockingBytes();

        InvalidationNotification notification = new InvalidationNotification(result);
        assertEquals(1, notification.getKeys().size());
        assertEquals("hello", new String(notification.getKeys().get(0).array(), Charset.defaultCharset()));
    }

    @Test
    void checkForInvalidationsTest() throws IOException, TimeoutException, InterruptedException, ParseException {
        CachedSocketConnection ctrl = new CachedSocketConnection("localhost", 6379);
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379);

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
        ByteBuffer result = con.readDataBlockingBytes();
        InvalidationNotification notification = new InvalidationNotification(result);
        assertEquals("hello", new String(notification.getKeys().get(0).array(), Charset.defaultCharset()));

    }

}