package com.github.rkaverin.console;

import com.beust.jcommander.Parameters;

@Parameters(commandNames = "rm", commandDescription = "remove registered directory from hash base")
public final class RemoveCommand extends AbstractDirCommand {

    @Override
    protected void doWork() throws CommandException {
        base.remove(dirPath);
    }

}
