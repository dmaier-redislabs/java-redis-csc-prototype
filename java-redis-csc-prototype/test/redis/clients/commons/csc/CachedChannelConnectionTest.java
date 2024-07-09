package redis.clients.commons.csc;

import org.junit.jupiter.api.Test;
import redis.clients.commons.csc.util.StopWatch;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CachedChannelConnectionTest {

    @Test
    void hasDataTest() throws Exception {
        CachedChannelConnection con = new CachedChannelConnection("localhost", 6379);
        con.getInner().write(ByteBuffer.wrap("PING\r\n".getBytes()));
        Thread.sleep(1000);
        assertEquals(true, con.hasData());
    }

    @Test
    void doesntHaveData() throws Exception {
        CachedChannelConnection con = new CachedChannelConnection("localhost", 6379);
        con.getInner().write(ByteBuffer.wrap("PING\r\n".getBytes()));
        assertEquals("+PONG\r\n",new String(con.readDataBlocking().array(), StandardCharsets.UTF_8));
        assertEquals(false, con.hasData());
    }

}