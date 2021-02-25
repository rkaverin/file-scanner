package com.github.rkaverin.console;

import com.beust.jcommander.Parameters;

@Parameters(commandNames = "purge", commandDescription = "purge hash base")
public final class PurgeCommand extends AbstractBaseCommand {

    @Override
    protected void doWork() throws CommandException {
        base.purge();
    }
}
