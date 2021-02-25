package com.github.rkaverin.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@EqualsAndHashCode
@Getter
public class FileEntry implements Serializable {
    private static final String NO_HASH = "no hash calculated yet";

    private String hash;
    private Path path;
    private long size;
    private FileTime creationTime;
    private FileTime modificationTime;


    public FileEntry(Path path) {
        this(path, NO_HASH);
    }

    public FileEntry(Path path, String hash) {
        this.path = path;
        this.hash = hash;

        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            this.creationTime = attributes.creationTime();
            this.modificationTime = attributes.lastModifiedTime();
            this.size = Files.size(path);
        } catch (IOException e) {
            this.creationTime = FileTime.fromMillis(0);
            this.modificationTime = FileTime.fromMillis(0);
            this.size = 0;
        }
    }

    //TODO: заменить сериализацию через строку на нормальную запись в объектный стрим
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
        hash = calcHash(path);
    }

    private static String calcHash(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (
                    InputStream is = Files.newInputStream(path);
                    DigestInputStream dis = new DigestInputStream(is, md);
                    OutputStream os = OutputStream.nullOutputStream()
            ) {
                dis.transferTo(os);
            }
            return byteArrayToString(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            return NO_HASH;
        }
    }

    private static String byteArrayToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public boolean isDone() {
        return !hash.equals(NO_HASH);
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


    public static boolean isSizeOrTimeChanged(FileEntry entry) {
        FileEntry onDisk = new FileEntry(entry.getPath());
        return entry.getSize() != onDisk.getSize()
                || entry.getCreationTime().toMillis() != onDisk.getCreationTime().toMillis()
                || entry.getModificationTime().toMillis() != onDisk.getModificationTime().toMillis();
    }


    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeObject(hash);
        stream.writeObject(path.toAbsolutePath().toString());
        stream.writeObject(size);
        stream.writeObject(creationTime.toInstant());
        stream.writeObject(modificationTime.toInstant());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        hash = (String) stream.readObject();
        path = Path.of((String) stream.readObject());
        size = (Long) stream.readObject();
        creationTime = FileTime.from((Instant) stream.readObject());
        modificationTime = FileTime.from((Instant) stream.readObject());
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

}
