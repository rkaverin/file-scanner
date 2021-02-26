package com.github.rkaverin.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
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

@ToString
@EqualsAndHashCode
@Getter
public class FileEntry implements Serializable {
    private static final String NO_HASH = "no hash calculated yet";
    private static final BasicFileAttributes NO_ATTR = new BasicFileAttributes() {
        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public FileTime lastAccessTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public FileTime creationTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public boolean isRegularFile() {
            return false;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public Object fileKey() {
            return null;
        }
    };

    private String hash;
    private final String path;
    private final long size;
    private final Instant creationTime;
    private final Instant modificationTime;


    public FileEntry(Path path) {
        this(path, NO_HASH);
    }

    public FileEntry(Path path, String hash) {
        BasicFileAttributes attributes = readAttributesOrEmpty(path);
        this.path = path.toString();
        this.hash = hash;
        this.creationTime = attributes.creationTime().toInstant();
        this.modificationTime = attributes.lastModifiedTime().toInstant();
        this.size = attributes.size();
    }

    private BasicFileAttributes readAttributesOrEmpty(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            return NO_ATTR;
        }
    }

    public void calcHash() {
        hash = calcHash(Path.of(path));
    }

    private static String calcHash(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (
                    DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), md);
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
        return hash.equals(other.getHash()) && this.isDone() && other.isDone();
    }

    public boolean isSamePath(FileEntry entry) {
        return this.getPath().equals(entry.getPath());
    }

    public static boolean isNotExists(FileEntry entry) {
        return !Files.exists(Path.of(entry.getPath()));
    }

    public static boolean isSizeOrTimeChanged(FileEntry entry) {
        FileEntry onDisk = new FileEntry(Path.of(entry.getPath()));
        return entry.getSize() != onDisk.getSize()
                || entry.getCreationTime().toEpochMilli() != onDisk.getCreationTime().toEpochMilli()
                || entry.getModificationTime().toEpochMilli() != onDisk.getModificationTime().toEpochMilli();
    }
}
