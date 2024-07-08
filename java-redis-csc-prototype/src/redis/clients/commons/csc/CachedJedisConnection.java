package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICacheKey;
import redis.clients.commons.csc.model.ICachedConnection;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


/**
 * A custom socket factory that allows us to get the socket
 */
class CustomJedisSocketFactory implements JedisSocketFactory {

    private Socket socket;
    private String host;
    private int port;

    public CustomJedisSocketFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Socket createSocket() throws JedisConnectionException {
        try {
            if (this.socket == null)
                this.socket = new Socket(this.host, this.port);
            return this.socket;
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }

    public Socket getSocket() {
        return this.socket;
    }

}


/**
 * Experimental wrapper class
 *
 */
public class CachedJedisConnection implements ICachedConnection<UnifiedJedis> {

    /**
     * The Jedis connection
     */
    private UnifiedJedis inner;
    private ICache cache;
    private Socket socket;


    /**
     * Cto
     *
     * @param host
     * @param port
     * @param cache
     */
    public CachedJedisConnection(String host, int port, ICache cache) {
        this.cache = cache;
        CustomJedisSocketFactory socketFactory = new CustomJedisSocketFactory(host, port);
        JedisClientConfig cfg = DefaultJedisClientConfig.builder().resp3().build();
        this.inner = new UnifiedJedis(socketFactory, cfg);
        //Socket is created when the first command is executed
        this.enableTracking();
        this.socket = socketFactory.getSocket();
    }

    @Override
    public UnifiedJedis getInner() {
        return this.inner;
    }

    @Override
    public ICache getCache() {
        return this.cache;
    }

    @Override
    public boolean enableTracking() {
        return this.inner.sendCommand(Protocol.Command.CLIENT, "TRACKING", "ON").toString().equals("OK");
    }

    /**
     * Wraps the set command
     * @param key
     * @param value
     * @return
     */
    String set(String key, String value) {
        return this.inner.set(key, value);
    }

    /**
     * Wraps the get command
     *
     * @param key
     * @return
     */
    String get(String key) {

        ICacheKey cacheKey = new StrCacheKey("GET", Arrays.asList(key));
        ByteBuffer value = this.cache.get(cacheKey);

        //Cache hit
        if (value != null) {

            //Check if there are any invalidations and process them
            if (this.hasData()) {
                try {
                    //Dirty hack to use the functionality of the already implemented CachedSocketConnection
                    CachedSocketConnection socketWrapper = new CachedSocketConnection(this.socket, 2000, this.cache, false);
                    InvalidationNotification msg = new InvalidationNotification(socketWrapper.readDataBlocking());
                    for (ByteBuffer k : msg.getKeys()) {
                        this.cache.deleteByRedisKey(k);
                    }
                } catch (IOException | ParseException | TimeoutException e) {
                    //Skip messages that can't be parsed
                }
            }

            return new String(value.array(), StandardCharsets.UTF_8);
        } else {
            //Cache miss
            //TODO: Separate invalidation messages from the response
            String valueStr = inner.get(key);
            this.cache.set(cacheKey, ByteBuffer.wrap(valueStr.getBytes(StandardCharsets.UTF_8)));
            return valueStr;
        }
    }

    /**
     * Check if the connection has any data
     *
     * @return
     */
    public boolean hasData()  {
        try {
            return this.socket.getInputStream().available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get the underlying socket
     * @return
     */
    public Socket getSocket() {
       return this.socket;
    }


}
