package com.github.rkaverin.commands;

import com.beust.jcommander.Parameter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractThreadedCommand extends AbstractDirCommand {
    @Parameter(names = {"-t", "--threads"}, description = "parallel threads to run")
    protected int threadCount = 1;

    protected ExecutorService executor;

    @Override
    protected void prepare() throws CommandException {
        super.prepare();
        executor = Executors.newFixedThreadPool(threadCount);
        if (isVerbose) {
            System.out.printf("Using %d worker threads%n", threadCount);
        }
    }

    @Override
    protected void cleanUp() throws CommandException {
        executor.shutdown();
        super.cleanUp();
    }
}
