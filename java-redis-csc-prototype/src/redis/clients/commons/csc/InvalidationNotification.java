package redis.clients.commons.csc;

import redis.clients.commons.csc.model.IKeyListPushNotification;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class InvalidationNotification implements IKeyListPushNotification {

    public static String TYPE = "invalidate";


    private String type;
    private ByteBuffer data;

    //TODO: This prototype assumes that the keys are strings.
    private final List<String> keys = new ArrayList<>();

    /**
     * Let's assume that we received the invalidation as binary data
     *s
     * @param data
     */
    public InvalidationNotification(ByteBuffer data) throws ParseException {
        this.data = data;
        this.parse();
    }

    @Override
    public String getType() {

        return this.type;
    }

    @Override
    public List<ByteBuffer> getKeys() {
        ArrayList<ByteBuffer> result = new ArrayList<>();

        for (String key : this.keys) {
            result.add(ByteBuffer.wrap(key.getBytes()));
        }

        return result;
    }

    @Override
    public ByteBuffer getData() {
        return this.data;
    }


    /**
     * Parse the data and extract the keys
     *
     * @throws ParseException
     */
    private void parse() throws ParseException {

        //Parse the "invalidate" preamble anc check if the number of keys is valid
        byte[] preamble = new byte[21];
        data.get(preamble);

        if (new String(preamble, Charset.defaultCharset()).equals(">2\r\n$10\r\ninvalidate\r\n")) {
            this.type = InvalidationNotification.TYPE;
        } else {
            this.type = "unknown";
            throw new ParseException("Invalid data", data.position());
        }

        //Parse the number of keys that are going to follow
        byte[] numKeysTemp = new byte[32];
        byte next;
        int i = 0;
        while ((next = data.get()) != '\r')  {
            if (next != '*') {
                numKeysTemp[i]=next;
                i++;
            }
        }

        byte[] numKeysTrimmed = new byte[i];
        ByteBuffer.wrap(numKeysTemp).slice(0, i).get(numKeysTrimmed);

        int numKeys = -1;
        try {
            numKeys = Integer.parseInt(new String(numKeysTrimmed, Charset.defaultCharset()));
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid number of keys", data.position());
        }

        //Read the newline character '\n'
        if (data.get() != '\n') {
            throw new ParseException("Invalid data", data.position());
        }

        //Parse the keys
        for (int j = 0; j < numKeys; j++) {

            //Skip the number of bytes to read
            while (data.get() != '\r') {}
            if (data.get() != '\n') {
                throw new ParseException("Invalid data", data.position());
            }

            //Keys can be very long, so using a StringBuilder here by assuming that the keys are strings
            StringBuilder sb = new StringBuilder();
            while ((next = data.get()) != '\r') {
                sb.append((char) next);
            }

            this.keys.add(sb.toString());

            if (data.get() != '\n') {
                throw new ParseException("Invalid data", data.position());
            }
        }
    }
}
