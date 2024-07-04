package redis.clients.commons.csc.util;

public class StopWatch {
    private long startTime = 0;
    private long endTime = 0;

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void stop() {
        this.endTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return this.endTime - this.startTime;
    }
}