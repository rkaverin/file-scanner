package com.github.rkaverin.console;

public interface Command {
    void execute() throws CommandException;
}
