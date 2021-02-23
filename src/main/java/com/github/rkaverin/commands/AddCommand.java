package com.github.rkaverin.commands;

import com.beust.jcommander.Parameters;

import java.io.IOException;

@Parameters(commandNames = "add", commandDescription = "add directory to hash file")
public final class AddCommand extends AbstractThreadedCommand {

    @Override
    public void doWork() throws CommandException {
        if (isVerbose) {
            System.out.printf("Add dir: %s%n", dirPath);
        }

        try {
            base.add(
                    dirPath,
                    executor,
                    getProgressBarCallback()
            );
        } catch (IOException | InterruptedException e) {
            throw new CommandException(e);
        }

        if (isVerbose) {
            System.out.printf(
                    "Completed. Scanned %d files (%d Mb) in %.0f seconds%n",
                    base.getFilesCount(dirPath),
                    base.getTotalByteCount(dirPath) / (1024 * 1024),
                    base.getScanTime()
            );
        }
    }

}
