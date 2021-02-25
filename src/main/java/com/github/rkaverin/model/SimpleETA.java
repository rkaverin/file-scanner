package com.github.rkaverin.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SimpleETA {

    private final List<Entry> data = new ArrayList<>();
    private double lastValue = -1;

    public void addProgress(double value) {
        if (value != lastValue) {
            data.add(new Entry(Instant.now(), value));
            lastValue = value;
        }
    }

    public double getETA() {
        Entry e0 = data.get(0);
        Entry e1 = data.get(data.size() - 1);

        return 100d * (e1.getTime().toEpochMilli() - e0.getTime().toEpochMilli()) / (e1.getValue() - e0.getValue()) / 1000;
    }

    @Data
    private static class Entry {
        private final Instant time;
        private final double value;
    }

}
