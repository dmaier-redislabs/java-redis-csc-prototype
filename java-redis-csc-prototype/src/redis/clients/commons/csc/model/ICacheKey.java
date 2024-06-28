package redis.clients.commons.csc.model;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A cache key is associated to the Redis command (as a String) and
 * the ordered list of Redis keys, whereby a key can be a binary value in
 * Redis.
 */
public interface ICacheKey {

    /**
     * @return The command that for which we cache the result
     */
    String getCmd();

    /**
     * @return The ordered list of Redis keys that were accessed
     */
    List<ByteBuffer> getRedisKeys();

    /**
     * Keys in Redis can have a binary format. So you need to be careful with this
     * method.
     * @return The String representation of the Redis keys.
     */
    List<String> getRedisKeyStr();

}
