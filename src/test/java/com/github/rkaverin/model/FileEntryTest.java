package com.github.rkaverin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.rkaverin.model.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class FileEntryTest {

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        FileEntry expected = new FileEntry(makeTestFile());
        expected.calcHash();

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(buffer)
        ) {
            output.writeObject(expected);

            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))
            ) {
                FileEntry actual = (FileEntry) input.readObject();
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    void serializationByJackson() throws IOException {
        FileEntry expected = new FileEntry(makeTestFile());
        expected.calcHash();
        FileEntry another = new FileEntry(makeAnotherTestFile());
        another.calcHash();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        String json = mapper
                .writeValueAsString(expected);

        FileEntry actual = mapper
                .readerFor(FileEntry.class)
                .readValue(json);

        assertEquals(expected, actual);
        assertNotEquals(another, actual);
    }

    @Test
    void calcHash() throws IOException {
        FileEntry entry1 = new FileEntry(makeTestFile());
        entry1.calcHash();
        FileEntry entry2 = new FileEntry(makeAnotherTestFile());
        entry2.calcHash();

        assertEquals(TEST_SHA_1, entry1.getHash());
        assertEquals(ANOTHER_TEST_SHA_1, entry2.getHash());
    }

    @Test
    void isDone() throws IOException {
        FileEntry entry = new FileEntry(makeTestFile());

        assertFalse(entry.isDone()); //пока хэш не посчитан isDone == false

        entry.calcHash();
        assertTrue(entry.isDone()); //хэш посчитан, isDone == true
    }

    @Test
    void isSameHash() throws IOException {
        FileEntry entry1 = new FileEntry(makeTestFile());
        FileEntry entry2 = new FileEntry(makeTestFile());
        FileEntry entry3 = new FileEntry(makeAnotherTestFile());

        //пока хэши не посчитаны файлы считаются разными
        assertFalse(entry1.isSameHash(entry2));
        assertFalse(entry1.isSameHash(entry3));

        entry1.calcHash();
        entry2.calcHash();
        entry3.calcHash();

        //когда хэши посчитаны сравнение работает корректно
        assertTrue(entry1.isSameHash(entry2));
        assertFalse(entry1.isSameHash(entry3));
    }

    @Test
    void isSamePath() throws IOException {
        Path path1_2 = makeTestFile();
        Path path3 = makeTestFile();
        FileEntry entry1 = new FileEntry(path1_2);
        FileEntry entry2 = new FileEntry(path1_2);
        FileEntry entry3 = new FileEntry(path3);

        assertTrue(entry1.isSamePath(entry2));
        assertFalse(entry1.isSamePath(entry3));
    }

    @Test
    void isNotExists() throws IOException {
        Path path = makeTestFile();
        FileEntry entry = new FileEntry(path);

        assertFalse(FileEntry.isNotExists(entry)); //файл на месте

        Files.delete(path);
        assertTrue(FileEntry.isNotExists(entry)); //файл отсутствует
    }

    @Test
    void isSizeOrTimeChanged() throws IOException {
        Path path = makeTestFile();
        FileEntry entry = new FileEntry(path);

        assertFalse(FileEntry.isSizeOrTimeChanged(entry)); //файл не изменен

        Files.newBufferedWriter(path).write(31337);
        assertTrue(FileEntry.isSizeOrTimeChanged(entry)); //размер файла и modification time файл изменились
    }

    @Test
    void invalidPath() {
        FileEntry entry = new FileEntry(Path.of(""));
        assertFalse(entry.isDone());

        entry.calcHash();
        assertFalse(entry.isDone());
    }
}