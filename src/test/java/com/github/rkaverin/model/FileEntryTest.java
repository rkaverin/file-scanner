package com.github.rkaverin.model;

import org.junit.jupiter.api.Test;

import java.io.*;

import static com.github.rkaverin.model.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class FileEntryTest {

    @Test
    void calcHash() throws IOException {
        FileEntry entry = new FileEntry(makeTestFile());
        entry.calcHash();

        assertEquals(TEST_SHA_1, entry.getHash());
    }

    @Test
    void isDone() throws IOException {
        FileEntry entry = new FileEntry(makeTestFile());
        assertFalse(entry.isDone());

        entry.calcHash();
        assertTrue(entry.isDone());
    }

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

}