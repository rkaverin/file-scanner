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
    private Map<Path, List<FileEntry>> map = new HashMap<>();
    private transient HashCalculator calculator;
    @Getter
    private transient boolean isModified = true;

    public void load(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            FileBase base = (FileBase) input.readObject();
            this.map = base.map;
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))) {
//            while (reader.ready()) {
//                try {
//                    Path dir = Path.of(reader.readLine());
//                    List<FileEntry> entry = new ArrayList<>();
//
//                    int count = Integer.parseInt(reader.readLine());
//                    for (int i = 0; i < count; i++) {
//                        entry.add(new FileEntry(reader.readLine()));
//                    }
//
//                    map.put(dir, entry);
//                } catch (NullPointerException ignored) {
//                }
//            }
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

            output.writeObject(this);

//        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))))) {
//            for (Map.Entry<Path, List<FileEntry>> entry : map.entrySet()) {
//                writer.write(entry.getKey().toString());
//                writer.newLine();
//                writer.write(Integer.toString(entry.getValue().size()));
//                writer.newLine();
//                for (FileEntry fileEntry : entry.getValue()) {
//                    writer.write(fileEntry.toString());
//                    writer.newLine();
//                }
//            }
            isModified = false;
        }
    }

    public void add(Path dir, ExecutorService executor, Runnable progressBarCallback) throws InterruptedException, IOException {
        List<FileEntry> list = new ArrayList<>();
        map.put(dir, list);
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
                        newFiles.add(entry.getPath());
                    }
                });

        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        calculator.getEntries().parallelStream()
                .forEach(entry -> {
                    List<Path> sameHashFiles = fileEntriesBySize.get(entry.getSize()).stream()
                            .filter(f -> f.isSameHash(entry))
                            .map(FileEntry::getPath)
                            .collect(toList());
                    if (sameHashFiles.isEmpty()) {
                        newFiles.add(entry.getPath());
                    } else {
                        duplicates.put(entry.getPath(), sameHashFiles);
                    }
                });
    }

    public void update(Path dir, ExecutorService executor, Runnable progressBarCallback, boolean fullscan) throws IOException, InterruptedException {
        List<FileEntry> list = new ArrayList<>(fullscan ? emptyList() : map.getOrDefault(dir, emptyList()));

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

        map.put(dir, list);
        isModified = true;
    }

    public Collection<Path> list() {
        return map.keySet().stream()
                .sorted()
                .collect(toList());
    }

    public void remove(Path path) {
        map.remove(path);
        isModified = true;
    }

    public long getFilesCount() {
        return map.values().stream()
                .mapToLong(List::size)
                .sum();
    }

    public long getFilesCount(Path path) {
        return map.getOrDefault(path, emptyList()).size();
    }

    public long getTotalByteCount() {
        return map.values().stream()
                .flatMap(Collection::stream)
                .mapToLong(FileEntry::getSize)
                .sum();
    }

    public long getTotalByteCount(Path path) {
        return map.getOrDefault(path, emptyList()).stream()
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

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(map.size());
        for (Map.Entry<Path, List<FileEntry>> entry : map.entrySet()
        ) {
            stream.writeObject(entry.getKey().toString());
            stream.writeObject(entry.getValue());
        }

    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        map = new HashMap<>();
        int cnt = (Integer) stream.readObject();
        while (cnt-- > 0) {
            Path path = Path.of((String) stream.readObject());
            //TODO: разобраться почему вылетает предупреждение и устранить его
            //noinspection unchecked
            List<FileEntry> list = (List<FileEntry>) stream.readObject();
            map.put(path, list);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileBase fileBase = (FileBase) o;
        return Objects.equals(map, fileBase.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}
