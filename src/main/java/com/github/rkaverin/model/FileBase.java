package com.github.rkaverin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@JsonIgnoreProperties({"totalByteCount", "modified", "eta", "progress", "scanSpeed", "scanTime", "filesCount"})
public class FileBase implements Serializable {
    @JsonProperty("map")
    private Map<String, List<FileEntry>> map = new HashMap<>();
    private transient HashCalculator calculator;
    @Getter
    private transient boolean isModified = true;

    @SuppressWarnings("unchecked") //Object to Map<String, List<FileEntry>>
    public void load(Path path) throws IOException, ClassNotFoundException {
//        try (ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
//            this.map = (Map<String, List<FileEntry>>) input.readObject();
//            isModified = false;
//        }

        try (InputStream input = new GZIPInputStream(Files.newInputStream(path))) {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.INDENT_OUTPUT);

            this.map = mapper
                    .readValue(input, new TypeReference<HashMap<String, List<FileEntry>>>() {});
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

//        try (ObjectOutputStream output = new ObjectOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
//            output.writeObject(this.map);
//            isModified = false;
//        }
        try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(path))) {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.INDENT_OUTPUT);

            mapper.writeValue(output, this.map);
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
                .forEach(list::add);

        calculator.add(list);
        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        isModified = true;
    }

    public void duplicates(Path dir, ExecutorService executor, Runnable progressBarCallback, List<Path> newFiles, Map<Path, List<Path>> duplicates) throws IOException, InterruptedException {
        Map<Long, List<FileEntry>> fileEntriesBySize = map.values().stream()
                .flatMap(Collection::stream)
                .collect(groupingBy(FileEntry::getSize));

        calculator = new HashCalculator(executor);

        List<FileEntry> possibleDuplicates = new ArrayList<>();

        Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(FileEntry::new)
                .forEach(entry -> {
                    if (fileEntriesBySize.containsKey(entry.getSize())) {
                        possibleDuplicates.add(entry);
                    } else {
                        newFiles.add(Path.of(entry.getPath()));
                    }
                });

        calculator.add(possibleDuplicates);
        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        possibleDuplicates.parallelStream()
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

        List<FileEntry> newFiles = Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(FileEntry::new)
                .parallel()
                .filter(entry -> list.parallelStream().noneMatch(entry::isSamePath))
                .collect(toList());

        calculator.add(newFiles);
        calculator.startCalc();
        calculator.awaitCalculation(progressBarCallback);

        list.addAll(newFiles);

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
