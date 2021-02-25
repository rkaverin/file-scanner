package com.github.rkaverin.console;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.IOException;

@Parameters(commandNames = "update", commandDescription = "scan a directory and update calculated hash base")
public final class UpdateCommand extends AbstractThreadedCommand {

    @Parameter(names = {"-f", "--fullscan"}, description = "force calc hash for all files in dir")
    boolean fullscan = false;

    @Override
    protected void doWork() throws CommandException {
        try {
            base.update(
                    dirPath,
                    executor,
                    getProgressBarCallback(),
                    fullscan
            );
        } catch (IOException | InterruptedException e) {
            throw new CommandException(e);
        }
    }

}
