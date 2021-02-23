package com.github.rkaverin.commands;

public interface Command {
    void execute() throws CommandException;
}
