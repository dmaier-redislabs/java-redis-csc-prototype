package redis.clients.commons.csc;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class InvalidationNotificationTest {


    @Test
    public void parseTest() throws ParseException {

        ByteBuffer data = ByteBuffer.wrap(">2\r\n$10\r\ninvalidate\r\n*1\r\n$5\r\nhello\r\n".getBytes());

        InvalidationNotification notification = new InvalidationNotification(data);
        assertEquals("invalidate", notification.getType());
        assertEquals(1, notification.getKeys().size());
        assertEquals("hello", new String(notification.getKeys().get(0).array(), Charset.defaultCharset()));
    }


}