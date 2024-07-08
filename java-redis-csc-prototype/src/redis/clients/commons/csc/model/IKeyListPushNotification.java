package redis.clients.commons.csc.model;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A push notification that contains a list of keys that are impacted
 */
public interface IKeyListPushNotification {


    static final String TYPE = "key list";

    /**
     * @return The type of message as string, e.g. 'invalidate'
     */
    String getType();

    /**
     * @return The list of keys that are impacted
     */
    List<ByteBuffer> getKeys();

    /**
     * @return The raw data of this notification
     */
    ByteBuffer getData();

}
