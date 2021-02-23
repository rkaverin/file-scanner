package com.github.rkaverin.commands;

import com.beust.jcommander.Parameters;

@Parameters(commandNames = "rm", commandDescription = "remove registered directory from hash base")
public final class RemoveCommand extends AbstractDirCommand {

    @Override
    public void doWork() throws CommandException {
        base.remove(dirPath);
    }

}
