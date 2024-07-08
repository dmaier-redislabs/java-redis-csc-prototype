package redis.clients.commons.csc;

import redis.clients.commons.csc.model.ICache;
import redis.clients.commons.csc.model.ICachedConnection;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class CachedSocketConnection implements ICachedConnection<Socket> {

    public static int BUFFER_SIZE = 1024;

    private final Socket socket;
    private final InputStream input;
    private final PrintWriter writer;
    private final ICache cache;
    private final long cmdTimeout;
    /**
     * The connection constructor
     *
     * @param host
     * @param port
     * @param cache
     * @throws IOException
     */
    public CachedSocketConnection(String host, int port, long cmdTimeout, ICache cache) throws IOException, TimeoutException {
        this.socket = new Socket(host, port);
        OutputStream output = socket.getOutputStream();
        this.input = socket.getInputStream();
        this.writer = new PrintWriter(output, true);
        this.cmdTimeout = cmdTimeout;
        this.cache = cache;
        this.enableTracking();
    }

    /**
     * The constructor that takes an already created socket
     *
     * @param socket
     * @param cmdTimeout
     * @param cache
     * @throws IOException
     */
    public CachedSocketConnection(Socket socket, long cmdTimeout, ICache cache, boolean trackingEnabled) throws IOException, TimeoutException {
        this.socket = socket;
        OutputStream output = this.socket.getOutputStream();
        this.input = socket.getInputStream();
        this.writer = new PrintWriter(output, true);
        this.cmdTimeout = cmdTimeout;
        this.cache = cache;
        if (trackingEnabled)
            this.enableTracking();
    }



    /**
     * Enable tracking on the Redis server
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public boolean enableTracking() {
        try {

            String helloResult = this.execRawCmdStr(new String[]{"HELLO", "3"});
            String trackingResult = this.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});

            if (helloResult.contains("redis") && trackingResult.contains("OK")) {
                return true;
            }

        } catch (IOException | TimeoutException e) {
            //Do nothing
        }

        return false;
    }


    /**
     * Execute a command by assuming that the passed data is a String and that the returned data is
     * also a String. This is only used for testing purposes. In the real world, Redis can handle binary
     * data, too.
     *
     * @param commandArr
     * @return The result of the command execution as String
     * @throws IOException
     * @throws TimeoutException
     */
    public String execRawCmdStr(String[] commandArr) throws IOException, TimeoutException {
        return new String(execRawCmdBin(commandArr).array(), Charset.defaultCharset());
    }

    /**
     * Execute a command by assuming that the passed data is a String and that the returned data is
     * a byte array wrapped by a byte buffer.
     *
     * @param commandArr
     * @return
     * @throws IOException
     */
    public ByteBuffer execRawCmdBin(String[] commandArr) throws IOException {

        StrCacheKey cacheKey = new StrCacheKey(commandArr[0],
                Arrays.asList(Arrays.copyOfRange(commandArr, 1, commandArr.length)));

        if (this.cache.hasCacheKey(cacheKey)) {

            ByteBuffer cachedValue = this.cache.get(cacheKey);

            //TODO: Just for experimenting
            //Check if there is an invalidation notification
            if (this.hasData()) {
                try {
                    InvalidationNotification msg = new InvalidationNotification(readDataBlocking());
                    for (ByteBuffer k : msg.getKeys()) {
                        this.cache.deleteByRedisKey(k);
                    }
                } catch (ParseException e) {
                    //Skipping because the received data is not an invalidation message
                }
            }

            return cachedValue;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("*").append(commandArr.length).append("\r\n");

            for (String c : commandArr) {
                sb.append("$").append(c.length()).append("\r\n");
                sb.append(c).append("\r\n");
            }

            this.writer.print(sb.toString());
            this.writer.flush();

            ByteBuffer data = readDataBlocking();

            if (this.cache.isCacheable(cacheKey)) {
                this.cache.set(cacheKey, data);
            }

            return data;
        }
    }

    /**
     * Check if the socket has any data
     *
     * @return True if there is data in the socket, false otherwise
     */
    public boolean hasData() {
        try {
            return input.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads data from the socket's input stream
     *
     * @return The data as ByteBuffer
     * @throws IOException

     */
    public ByteBuffer readDataBlocking() throws IOException {

        this.socket.setSoTimeout((int) cmdTimeout);
        ByteArrayOutputStream baas = new ByteArrayOutputStream();

        //Block until data arrives. If no data arrives, a SocketTimeOutException is raised.
        byte[] tmpBuffer = new byte[BUFFER_SIZE];
        int bytesRead = this.input.read(tmpBuffer);
        baas.write(tmpBuffer, 0, bytesRead);

        //Avoid unnecessary read blocking if there isn't any additional data.
        //If there is more data, then write it into the byte array output stream.
        while (this.input.available() > 0) {
            tmpBuffer = new byte[BUFFER_SIZE];
            bytesRead = this.input.read(tmpBuffer);
            baas.write(tmpBuffer, 0, bytesRead);
        }

        return ByteBuffer.wrap(baas.toByteArray());
    }


    @Override
    public Socket getInner() {
        return this.socket;
    }

    @Override
    public ICache getCache() {
        return this.cache;
    }
}
