package com.github.rkaverin.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.rkaverin.model.TestUtils.makeTestDir;
import static com.github.rkaverin.model.TestUtils.makeTestFile;
import static org.junit.jupiter.api.Assertions.*;

class FileBaseTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterAll
    static void tearDown() {
        executor.shutdown();
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException, InterruptedException {
        FileBase expected = new FileBase();
        expected.add(makeTestDir(), executor, () -> {});
        expected.add(makeTestDir(), executor, () -> {});
        expected.add(makeTestDir(), executor, () -> {});

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(buffer)
        ) {
            output.writeObject(expected);

            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))
            ) {
                FileBase actual = (FileBase) input.readObject();
                assertTrue(actual.isEqualBase(expected));
            }
        }
    }

    @Test
    void saveAndLoad() throws IOException, InterruptedException, ClassNotFoundException {
        FileBase expected = new FileBase();
        FileBase actual = new FileBase();

        expected.add(makeTestDir(), executor, () -> {});
        expected.add(makeTestDir(), executor, () -> {});
        expected.add(makeTestDir(), executor, () -> {});

        Path path = Files.createTempFile("fscan-base", null);
        expected.save(path);

        assertFalse(actual.isEqualBase(expected)); //базы разные

        actual.load(path);
        assertTrue(actual.isEqualBase(expected)); //базы эквивалентны


    }


}