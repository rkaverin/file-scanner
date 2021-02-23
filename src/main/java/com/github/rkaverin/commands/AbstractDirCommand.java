package com.github.rkaverin.commands;

import com.beust.jcommander.Parameter;
import com.github.rkaverin.AbsolutePathConverter;

import java.nio.file.Path;

public abstract class AbstractDirCommand extends AbstractBaseCommand {
    @Parameter(description = "directory to scan", converter = AbsolutePathConverter.class)
    protected Path dirPath; //TODO: переделать в List<Path> dirs

}
