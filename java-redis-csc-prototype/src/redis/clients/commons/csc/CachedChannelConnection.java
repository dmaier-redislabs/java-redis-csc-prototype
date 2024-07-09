package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICachedConnection;
import redis.clients.commons.csc.util.StopWatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class CachedChannelConnection implements ICachedConnection<SocketChannel> {

    //Uses a SocketChannel instead of a plain Socket, whereby this seems to be a wrapper around a Java Socket (see getSocket)
    public static final int BUFFER_SIZE = 1024;
    public static final int TIMEOUT = 2000;

    private SocketChannel inner;
    private Selector selector;

    public CachedChannelConnection(String host, int port) throws IOException {
        this.inner = SocketChannel.open();
        this.inner.connect(new InetSocketAddress(host, port));
        this.inner.configureBlocking(false);
        this.selector = Selector.open();
        this.inner.register(selector, SelectionKey.OP_READ);
    }

    @Override
    public SocketChannel getInner() {
        return this.inner;
    }

    @Override
    public ICache getCache() {
        return null;
    }

    @Override
    public boolean enableTracking() {
        return false;
    }


    /**
     * Reads data from the channel
     *
     * @return The data as ByteBuffer
     * @throws IOException

     */
    public ByteBuffer readDataBlocking() throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        StopWatch sw = new StopWatch();
        sw.start();

        ByteBuffer tmpBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        long elapsed = -1;

        //We expect some data to appear or a timeout to happen. The read command is now non-blocking, so we should
        //surround it with a waiting loop that checks if we received any data. If we don't receive any data, then
        //the timeout is reached, and we throw an exception.
        while (result.size() == 0 && elapsed < TIMEOUT) {

            int bytesRead = -1;

            while (hasData()) {
                tmpBuffer.clear();
                bytesRead = this.inner.read(tmpBuffer);
                result.write(tmpBuffer.array(), 0, bytesRead);
            }

            sw.stop();
            elapsed = sw.getElapsedTime();
        }

        if (elapsed >= TIMEOUT) {
            throw new SocketTimeoutException("Timeout while reading data from the channel");
        } else {
            return ByteBuffer.wrap(result.toByteArray());
        }
    }


    @Override
    public boolean hasData() {
        try {
            return selector.selectNow() == 1;
        } catch (IOException e) {
            return false;
        }
    }
}
