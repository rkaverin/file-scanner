package com.github.rkaverin.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HashCalculator {
    private final Map<FileEntry, Future<?>> entries = new HashMap<>();
    private final ExecutorService executor;
    private final SimpleTimer timer = new SimpleTimer(false);
    private final SimpleETA eta = new SimpleETA();

    public HashCalculator(ExecutorService executor) {
        this.executor = executor;
    }

    public void add(FileEntry entry) {
        entries.put(entry, null);
    }

    public Collection<? extends FileEntry> getEntries() {
        return entries.keySet();
    }

    public void startCalc() {
        entries.entrySet().stream()
                .parallel()
                .forEach(entry -> entry.setValue(executor.submit(() -> entry.getKey().calcHash())));
        timer.start();
        eta.addProgress(0d);
    }

    public void awaitCalculation(Runnable progressBarCallback) throws InterruptedException {
        while (!entries.values().stream().allMatch(Future::isDone)) {
            TimeUnit.MILLISECONDS.sleep(300);
            eta.addProgress(getProgress());
            progressBarCallback.run();
        }
        timer.stop();
    }

    private long getTotalByteCount() {
        return entries.keySet().stream()
                .mapToLong(FileEntry::getSize)
                .sum();
    }

    private long getProcessedByteCount() {
        return entries.keySet().stream()
                .filter(FileEntry::isDone)
                .mapToLong(FileEntry::getSize)
                .sum();
    }

    public double getProgress() {
        return 100d * getProcessedByteCount() / getTotalByteCount();
    }

    public double getETA() {
//        return (getTotalByteCount() * 1.0 / getProcessedByteCount() - 1) * getScanTime();
        return eta.getETA();
    }

    public double getScanTime() {
        return timer.getSeconds();
    }

    public double getScanSpeed() {
        return getProcessedByteCount() * 1.0 / (1024 * 1024 * getScanTime());
    }
}
