package com.github.rkaverin;

import com.beust.jcommander.IStringConverter;

import java.nio.file.Path;

public class AbsolutePathConverter implements IStringConverter<Path> {
    @Override
    public Path convert(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }
}
