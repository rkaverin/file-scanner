package com.github.rkaverin.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.rkaverin.model.TestUtils.TEST_SHA_1;
import static com.github.rkaverin.model.TestUtils.makeTestFile;
import static org.junit.jupiter.api.Assertions.*;


class HashCalculatorTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterAll
    static void tearDown() {
        executor.shutdown();
    }

    @Test
    void addAndGetEntries() throws IOException {
        HashCalculator calculator = new HashCalculator(executor);

        List<FileEntry> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new FileEntry(makeTestFile()));
        }
        list.forEach(calculator::add);

        assertTrue(calculator.getEntries().containsAll(list));
        assertTrue(list.containsAll(calculator.getEntries()));
    }

    @Test
    void calcAndAwait() throws IOException, InterruptedException {
        HashCalculator calculator = new HashCalculator(executor);

        for (int i = 0; i < 1000; i++) {
            calculator.add(new FileEntry(makeTestFile()));
        }

        calculator.startCalc();
        calculator.awaitCalculation(() -> {});

        boolean allDone = calculator.getEntries().stream()
                .allMatch(FileEntry::isDone);
        assertTrue(allDone);

        boolean allHashEquals = calculator.getEntries().stream()
                .allMatch(entry -> entry.getHash().equals(TEST_SHA_1));
        assertTrue(allHashEquals);
    }
}