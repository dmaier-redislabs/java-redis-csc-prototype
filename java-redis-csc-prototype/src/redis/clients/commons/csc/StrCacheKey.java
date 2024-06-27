package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICacheKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A cache key that assumes that the keys are strings
 *
 * TODO: This is more for testing. We should ensure that we have a binary version, too.
 */
public class StrCacheKey implements ICacheKey {

    private final String cmd;
    private final List<String> redisKeys;

    /**
     * The one and only constructor
     *
     * @param cmd
     * @param redisKeys
     */
    public StrCacheKey(String cmd, List<String> redisKeys) {
        this.cmd = cmd;
        this.redisKeys = redisKeys;
    }

    @Override
    public String getCmd() {
        return this.cmd;
    }

    @Override
    public List<ByteBuffer> getRedisKeys() {
        List<ByteBuffer> result = new ArrayList<>();
        for (String str : this.redisKeys) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
            result.add(byteBuffer);
        }
        return result;
    }

    @Override
    public List<String> getRedisKeyStr() {
       return this.redisKeys;
    }
}
