package com.github.rkaverin.files;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class FileBase {
    private final Map<Path, List<FileEntry>> map = new HashMap<>();

    private HashCalculator calculator;

//    private Path loadedFrom;

// --Commented out by Inspection START (19.02.2021, 23:27):
//    public Path getLoadedFrom() {
//        return loadedFrom;
//    }
// --Commented out by Inspection STOP (19.02.2021, 23:27)

    /**
     * Загружает базу из файла.
     * Формат файла текстовый.
     * <li>Просканированный каталог, Path</li>
     * <li>Количество файлов в списке, Integer</li>
     * <li>Строка с объектом FileEntry</li>
     *
     * @param path файл с базой
     */
    public void load(Path path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))) {
            while (reader.ready()) {
                try {
                    Path dir = Path.of(reader.readLine());
                    List<FileEntry> entry = new ArrayList<>();

                    int count = Integer.parseInt(reader.readLine());
                    for (int i = 0; i < count; i++) {
                        entry.add(new FileEntry(reader.readLine()));
                    }

                    map.put(dir, entry);
                } catch (NullPointerException ignored) {
                }
            }
//            loadedFrom = path;
        }
    }

//    public void save() throws IOException {
//        save(loadedFrom);
//    }

    public void save(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir)) {
            if (!parentDir.toFile().mkdirs()) {
                throw new IOException("can't create dir " + parentDir);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))))) {
            for (Map.Entry<Path, List<FileEntry>> entry : map.entrySet()) {
                writer.write(entry.getKey().toString());
                writer.newLine();
                writer.write(Integer.toString(entry.getValue().size()));
                writer.newLine();
                for (FileEntry fileEntry : entry.getValue()) {
                    writer.write(fileEntry.toString());
                    writer.newLine();
                }
            }
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
    }

    public void duplicates(Path dir, ExecutorService executor, Runnable progressBarCallback, List<Path> newFiles, Map<Path, List<Path>> duplicates) throws IOException, InterruptedException {
/*
    первый проход - находим точно новые файлы и кандидатов в дубликаты. Новые файлы сразу в список новых. Кандидатов в калькулятор
    запускаем расчет хэшей в калькуляторе
    второй проход - по посчитанному списку хэшей сортируем на новые файлы и дубликаты
*/

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
    }

    public Collection<Path> list() {
        return map.keySet().stream()
                .sorted()
                .collect(toList());
    }

    public void remove(Path path) {
        map.remove(path);
    }

    /**
     * Возвращает количество файлов в базе
     *
     * @return количество файлов
     */
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
    }
}
