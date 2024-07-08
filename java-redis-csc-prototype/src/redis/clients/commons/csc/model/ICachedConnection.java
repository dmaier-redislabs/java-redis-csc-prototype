package redis.clients.commons.csc.model;

/**
 * A simple wrapper for connections
 *
 * @param <T> The type of the inner connection
 */
public interface ICachedConnection<T> {

    /**
     *  We are going to use a generic connection here, but will test it with a simple TCP socket later
     * @return The actual connection that is used (either a Jedis connection or Lettuce connection)
     */
    T getInner();

    /**
     * @return The cache that belongs to this connection
     */
    ICache getCache();

    /**
     * Enable tracking on the Redis server
     */
    boolean enableTracking();
}
