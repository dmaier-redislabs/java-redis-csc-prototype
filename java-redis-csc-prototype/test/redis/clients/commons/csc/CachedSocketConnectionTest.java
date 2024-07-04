package redis.clients.commons.csc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

class CachedSocketConnectionTest {

    @Test
    void execRawCmdStrTest() throws IOException, TimeoutException, InterruptedException {
        CachedSocketConnection con = new CachedSocketConnection("localhost", 6379);

        con.execRawCmdStr(new String[]{"HELLO", "3"});
        con.execRawCmdStr(new String[]{"CLIENT", "TRACKING", "ON"});
        con.execRawCmdStr(new String[]{"SET", "hello", "world"});
        String result = con.execRawCmdStr(new String[]{"GET", "hello"});
        System.out.println(result);

        while (!con.hasData()) {
            System.out.println("Waiting for data...");
            Thread.sleep(100);
        }

        System.out.println("Found some data!");
        result = con.blockAndReadData();
        System.out.println(result);


    }
}