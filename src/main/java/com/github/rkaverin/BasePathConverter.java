package com.github.rkaverin;

import com.beust.jcommander.IStringConverter;

import java.nio.file.Path;

public class BasePathConverter implements IStringConverter<Path> {
    @Override
    public Path convert(String value) {
        Path result;
        if (Path.of(value).getFileName().toString().equals(Path.of(value).toString())) {
            result = Path.of(System.getProperty("user.home"))
                    .resolve("./.file-scanner/")
                    .resolve(value)
                    .normalize();
        } else {
            result = Path.of(value);
        }
        return result;
    }
}
