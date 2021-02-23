package com.github.rkaverin.files;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

@EqualsAndHashCode
@Getter
public class FileEntry {
    private String hash;
    private final Path path;
    private final long size;
    private final FileTime creationTime;
    private final FileTime modificationTime;


    public FileEntry(Path path) {
        this(path, "");
    }

    //TODO: неизящный конструктор, надо придумать как сделать аккуратнее
    public FileEntry(Path path, String hash) {
        FileTime creationTime;
        FileTime modificationTime;
        long size;
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            creationTime = attributes.creationTime();
            modificationTime = attributes.lastModifiedTime();
            size = Files.size(path);
        } catch (IOException e) {
            size = 0;
            creationTime = FileTime.fromMillis(0);
            modificationTime = FileTime.fromMillis(0);
        }
        this.path = path;
        this.hash = hash;
        this.creationTime = creationTime;
        this.modificationTime = modificationTime;
        this.size = size;
    }

    public FileEntry(String line) {
        this(
                Path.of(line.split("\t")[0]),
                Long.parseLong(line.split("\t")[1]),
                FileTime.fromMillis(Long.parseLong(line.split("\t")[2])),
                FileTime.fromMillis(Long.parseLong(line.split("\t")[3])),
                line.split("\t")[4]
        );
    }

    private FileEntry(Path path, long size, FileTime creationTime, FileTime modificationTime, String hash) {
        this.path = path;
        this.size = size;
        this.creationTime = creationTime;
        this.modificationTime = modificationTime;
        this.hash = hash;
    }

    public void calcHash() {
        hash = Utils.calcFileMd5(path);
    }

    public boolean isDone() {
        return !hash.isEmpty();
    }

    public boolean isSameHash(FileEntry other) {
        return hash.equals(other.getHash()) && !hash.isEmpty() && !other.getHash().isEmpty();
    }

    public boolean isSamePath(FileEntry entry) {
        return this.getPath().equals(entry.getPath());
    }

    public static boolean isNotExists(FileEntry entry) {
        return !Files.exists(entry.getPath());
    }


    @Override
    public String toString() {
        return String.format(
                "%s\t%d\t%d\t%d\t%s",
                getPath().toString(),
                getSize(),
                getCreationTime().toMillis(),
                getModificationTime().toMillis(),
                getHash()
        );
    }

    public static boolean isSizeOrTimeChanged(FileEntry entry) {
        FileEntry onDisk = new FileEntry(entry.getPath());
        return entry.getSize() != onDisk.getSize()
                || entry.getCreationTime().toMillis() != onDisk.getCreationTime().toMillis()
                || entry.getModificationTime().toMillis() != onDisk.getModificationTime().toMillis();
    }
}
