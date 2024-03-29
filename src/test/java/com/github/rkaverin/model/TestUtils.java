package com.github.rkaverin.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {
    static final String TEST_SHA_1 = "5b00669c480d5cffbdfa8bdba99561160f2d1b77";
    static final String ANOTHER_TEST_SHA_1 = "c423e794187f6c5b1d17066a23bdbaacbaf697fe";

    public static void main(String[] args) throws IOException {
        System.out.println(makeAnotherTestFile());
    }

    static Path makeTestFile() throws IOException {
        Path result = Files.createTempFile("fscan", null);
        try (OutputStream output = Files.newOutputStream(result)) {
            for (int i = 0; i < 1024; i++) {
                output.write(i);
            }
        }
        return result;
    }

    static Path makeAnotherTestFile() throws IOException {
        Path result = Files.createTempFile("fscan", null);
        try (OutputStream output = Files.newOutputStream(result)) {
            for (int i = 1024; i > 0; i--) {
                output.write(i);
            }
        }
        return result;
    }

    static Path makeRandomTestFile() throws IOException {
        Path result = Files.createTempFile("fscan", null);
        try (OutputStream output = Files.newOutputStream(result)) {
            for (int i = 1024; i > 0; i--) {
                output.write((int) (Math.random() * 256));
            }
        }
        return result;
    }


    static Path makeTestDir() throws IOException {
        Path result = Files.createTempDirectory("fscan-dir");

        for (int i = 0; i < 1000; i++) {
            Path path = Files.createTempFile(result, "fscan", null);
            try (OutputStream output = Files.newOutputStream(path)) {
                for (int j = 0; j < 1024; j++) {
                    output.write(j);
                }
            }
        }
        return result;
    }

    static Runnable noProgressBar() {
        return () -> {};
    }
}
