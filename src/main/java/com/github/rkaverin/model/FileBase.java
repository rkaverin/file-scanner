package com.github.rkaverin.model;

import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class FileBase implements Serializable {
    private Map<String, List<FileEntry>> map = new HashMap<>();
    private transient HashCalculator calculator;
    @Getter
    private transient boolean isModified = true;

    @SuppressWarnings("unchecked") //Object to Map<String, List<FileEntry>>
    public void load(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            this.map = (Map<String, List<FileEntry>>) input.readObject();
            isModified = false;
        }
    }

    public void save(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir)) {
            if (!parentDir.toFile().mkdirs()) {
                throw new IOException("can't create dir " + parentDir);
            }
        }

        try (ObjectOutputStream output = new ObjectOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
            output.writeObject(this.map);
            isModified = false;
        }
    }

    public void add(Path dir, ExecutorService executor, Runnable progressBarCallback) throws InterruptedException, IOException {
        List<FileEntry> list = new ArrayList<>();
        map.put(dir.toString(), list);
        calculator = new HashCalculator(executor);

        Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(FileEntry::new)
                .forEach(calculator::add);

        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        list.addAll(calculator.getEntries());

        isModified = true;
    }

    public void duplicates(Path dir, ExecutorService executor, Runnable progressBarCallback, List<Path> newFiles, Map<Path, List<Path>> duplicates) throws IOException, InterruptedException {
        Map<Long, List<FileEntry>> fileEntriesBySize = map.values().stream()
                .flatMap(Collection::stream)
                .collect(groupingBy(FileEntry::getSize));

        calculator = new HashCalculator(executor);

        Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(FileEntry::new)
                .forEach(entry -> {
                    if (fileEntriesBySize.containsKey(entry.getSize())) {
                        calculator.add(entry);
                    } else {
                        newFiles.add(Path.of(entry.getPath()));
                    }
                });

        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        calculator.getEntries().parallelStream()
                .forEach(entry -> {
                    List<Path> sameHashFiles = fileEntriesBySize.get(entry.getSize()).stream()
                            .filter(f -> f.isSameHash(entry))
                            .map(FileEntry::getPath)
                            .map(Path::of)
                            .collect(toList());
                    if (sameHashFiles.isEmpty()) {
                        newFiles.add(Path.of(entry.getPath()));
                    } else {
                        duplicates.put(Path.of(entry.getPath()), sameHashFiles);
                    }
                });
    }

    public void update(Path dir, ExecutorService executor, Runnable progressBarCallback, boolean fullscan) throws IOException, InterruptedException {
        List<FileEntry> list = new ArrayList<>(fullscan ? emptyList() : map.getOrDefault(dir.toString(), emptyList()));

        list.removeIf(FileEntry::isNotExists);
        list.removeIf(FileEntry::isSizeOrTimeChanged);

        calculator = new HashCalculator(executor);

        Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(FileEntry::new)
                .parallel()
                .filter(entry -> list.parallelStream().noneMatch(entry::isSamePath))
                .forEach(calculator::add);

        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        list.addAll(calculator.getEntries());

        map.put(dir.toString(), list);
        isModified = true;
    }

    public Collection<Path> list() {
        return map.keySet().stream()
                .sorted()
                .map(Path::of)
                .collect(toList());
    }

    public void remove(Path path) {
        map.remove(path.toString());
        isModified = true;
    }

    public long getFilesCount() {
        return map.values().stream()
                .mapToLong(List::size)
                .sum();
    }

    public long getFilesCount(Path path) {
        return map.getOrDefault(path.toString(), emptyList()).size();
    }

    public long getTotalByteCount() {
        return map.values().stream()
                .flatMap(Collection::stream)
                .mapToLong(FileEntry::getSize)
                .sum();
    }

    public long getTotalByteCount(Path path) {
        return map.getOrDefault(path.toString(), emptyList()).stream()
                .mapToLong(FileEntry::getSize)
                .sum();
    }

    public double getETA() {
        return calculator.getETA();
    }

    public double getProgress() {
        return calculator.getProgress();
    }

    public double getScanSpeed() {
        return calculator.getScanSpeed();
    }

    public double getScanTime() {
        return calculator.getScanTime();
    }

    public void purge() {
        map.clear();
        isModified = true;
    }

    public boolean isEqualBase(FileBase other) {
        return this.map.equals(other.map);
    }

}
