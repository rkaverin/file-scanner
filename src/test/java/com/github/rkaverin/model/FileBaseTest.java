package com.github.rkaverin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.rkaverin.model.TestUtils.makeTestDir;
import static com.github.rkaverin.model.TestUtils.noProgressBar;
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
        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());

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
    void serializationByJackson() throws IOException, InterruptedException {
        FileBase expected = new FileBase();
        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        String json = mapper
                .writeValueAsString(expected);

        FileBase actual = mapper
                .readerFor(FileBase.class)
                .readValue(json);

        assertTrue(actual.isEqualBase(expected));
    }

    @Test
    void saveAndLoad() throws IOException, InterruptedException, ClassNotFoundException {
        FileBase expected = new FileBase();
        FileBase actual = new FileBase();

        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());
        expected.add(makeTestDir(), executor, noProgressBar());

        Path path = Files.createTempFile("fscan-base", null);
        expected.save(path);

        assertFalse(actual.isEqualBase(expected)); //базы разные

        actual.load(path);
        assertTrue(actual.isEqualBase(expected)); //базы эквивалентны
    }

    @Test
    void add() throws IOException, InterruptedException {
        FileBase base = new FileBase();

        base.add(makeTestDir(), executor, noProgressBar());
        base.add(makeTestDir(), executor, noProgressBar());
        base.add(makeTestDir(), executor, noProgressBar());

        assertEquals(1024 * 1000 * 3, base.getTotalByteCount());
    }

    @Test
    void list() throws IOException, InterruptedException {
        FileBase base = new FileBase();
        List<Path> list = new ArrayList<>();
        list.add(makeTestDir());
        list.add(makeTestDir());
        list.add(makeTestDir());

        assertEquals(0, base.list().size());

        for (Path path : list) {
            base.add(path, executor, noProgressBar());
        }
        assertEquals(3, base.list().size());
        assertTrue(base.list().containsAll(list));
    }
}