package com.github.rkaverin.console;

import com.beust.jcommander.Parameter;

public abstract class AbstractCommand implements Command {

    @Parameter(names = {"-v", "--verbose"}, description = "verbose output")
    protected boolean isVerbose = false;

    @Override
    public void execute() throws CommandException {
        prepare();
        doWork();
        cleanUp();
    }

    protected abstract void prepare() throws CommandException;

    protected abstract void doWork() throws CommandException;

    protected abstract void cleanUp() throws CommandException;
}
