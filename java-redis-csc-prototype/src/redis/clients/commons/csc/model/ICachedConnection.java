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
     * @return True if tracking was enabled, false otherwise
     */
    boolean enableTracking();


    /**
     * @return True if the connection has data still to be processed, false otherwise.
     *
     * This is espeically needed in the context of push notifications, where we need to know if there are still messages to be processed
     */
    boolean hasData();
}
