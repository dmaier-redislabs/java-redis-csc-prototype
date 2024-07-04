package redis.clients.commons.csc;

import redis.clients.commons.csc.util.StopWatch;

import java.io.*;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.concurrent.TimeoutException;

public class CachedSocketConnection {

    public static long CMD_TIMEOUT = 2000;
    private Socket socket;
    private OutputStream output;
    private InputStream input;
    private PrintWriter writer;
    private BufferedReader reader;

    /**
     * The connection constructor
     *
     * @param host
     * @param port
     * @throws IOException
     */
    public CachedSocketConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.output = socket.getOutputStream();
        this.input = socket.getInputStream();
        this.writer = new PrintWriter(output, true);
        this.reader = new BufferedReader(new InputStreamReader(input));
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
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(commandArr.length).append("\r\n");

        for (String c : commandArr) {
            sb.append("$").append(c.length()).append("\r\n");
            sb.append(c).append("\r\n");
        }

        this.writer.print(sb.toString());
        this.writer.flush();

        return blockAndReadData();
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
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Wait for data and then read it from the socket's input stream
     *
     * @return The data as String
     * @throws TimeoutException
     * @throws IOException
     */
    public String blockAndReadData() throws TimeoutException, IOException {
        long elapsed = 0;
        int numBytes = 0;
        StopWatch sw = new StopWatch();

        sw.start();
        while (numBytes == 0 && elapsed < CMD_TIMEOUT) {
            numBytes = this.input.available();
            sw.stop();
            elapsed = sw.getElapsedTime();
        }

        if (elapsed >= CMD_TIMEOUT) {
            throw new TimeoutException("Command timed out");
        } else {
            CharBuffer buffer = CharBuffer.allocate(numBytes);
            int consumed = this.reader.read(buffer);
            return new String(buffer.array(), 0, consumed);
        }
    }
}
