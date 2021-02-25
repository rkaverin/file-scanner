package com.github.rkaverin.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SimpleTimer {
    private long t0;
    private long t1;
    private final List<Interval> intervals = new ArrayList<>();
    private boolean isRunning;

    public SimpleTimer(boolean isRunning) {
        reset();
        this.isRunning = isRunning;
    }

    private long getNowMillis() {
        return Instant.now().toEpochMilli();
    }

    public void reset() {
        intervals.clear();
        t0 = getNowMillis();
        t1 = t0;
    }

    public void start() {
        isRunning = true;
        t0 = getNowMillis();
    }

    public void stop() {
        isRunning = false;
        t1 = getNowMillis();
        intervals.add(new Interval(t0, t1));
    }

    public double getSeconds() {
        return 1.0 * getMillis() / 1000;
    }

    public long getMillis() {
        if (isRunning) {
            return getNowMillis() - t0 +
                    intervals.stream()
                            .mapToLong(Interval::getLength)
                            .sum();

        } else {
            return intervals.stream()
                    .mapToLong(Interval::getLength)
                    .sum();
        }
    }

    private static class Interval {
        private final long start;
        private final long stop;

        public Interval(long start, long stop) {
            this.start = start;
            this.stop = stop;
        }

        public long getLength() {
            return stop - start;
        }
    }
}
