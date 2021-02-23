package com.github.rkaverin.commands;

import com.beust.jcommander.Parameter;
import com.github.rkaverin.BasePathConverter;
import com.github.rkaverin.files.FileBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractBaseCommand extends AbstractCommand {

    @Parameter(names = {"-b", "--base"}, description = "file to store calculated hash", converter = BasePathConverter.class)
    protected Path basePath; //TODO: переделать в FileBase base

    protected FileBase base;

    @Override
    protected void prepare() throws CommandException {
        base = loadBase(basePath);
    }

    @Override
    protected void cleanUp() throws CommandException {
        saveBase(base, basePath);
    }

    protected FileBase loadBase(Path path) throws CommandException {
        FileBase result = new FileBase();
        if (isVerbose) {
            System.out.printf("Using base %s%n", path);
        }

        if (Files.exists(path)) {
            try {
                result.load(path);
                if (isVerbose) {
                    System.out.printf("Loaded %d file hashes%n", result.getFilesCount());
                }
            } catch (IOException e) {
                throw new CommandException(e);
            }
        }

        return result;
    }

    protected void saveBase(FileBase fileBase, Path path) throws CommandException {
        try {
            fileBase.save(path);
        } catch (IOException e) {
            throw new CommandException(e);
        }
        if (isVerbose) {
            System.out.printf("Base saved to %s%n", path);
        }
    }

    protected Runnable getProgressBarCallback() {
        return () -> System.out.printf(
                "Progress % 3.1f%% [% 5.2f Mb/s] [Elapsed: % 4.0f] [ETA: % 4.0f]                    \r",
                base.getProgress(),
                base.getScanSpeed(),
                base.getScanTime(),
                base.getETA()
        );
    }
}
