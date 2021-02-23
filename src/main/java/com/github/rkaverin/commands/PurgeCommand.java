package com.github.rkaverin.commands;

import com.beust.jcommander.Parameters;

@Parameters(commandNames = "purge", commandDescription = "purge hash base")
public final class PurgeCommand extends AbstractBaseCommand {

    @Override
    public void doWork() throws CommandException {
        base.purge();
    }
}
