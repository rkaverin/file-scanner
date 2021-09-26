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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HashCalculatorTest {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterAll
    static void tearDown() {
        executor.shutdown();
    }

    @Test
    void add() throws IOException {
        HashCalculator calculator = new HashCalculator(executor);

        List<FileEntry> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new FileEntry(makeTestFile()));
        }

        assertEquals(0, calculator.getCount());
        calculator.add(list);
        assertEquals(1000, calculator.getCount());
    }

    @Test
    void calcAndAwait() throws IOException, InterruptedException {
        HashCalculator calculator = new HashCalculator(executor);

        List<FileEntry> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new FileEntry(makeTestFile()));
        }

        calculator.add(list);
        calculator.startCalc();
        calculator.awaitCalculation(() -> {
        });

        boolean allDone = list.stream().allMatch(FileEntry::isDone);
        assertTrue(allDone);

        boolean allHashEquals = list.stream()
                .allMatch(entry -> entry.getHash().equals(TEST_SHA_1));
        assertTrue(allHashEquals);
    }
}