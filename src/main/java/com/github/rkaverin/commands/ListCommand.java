package com.github.rkaverin.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = "ls", commandDescription = "list registered directories ")
public final class ListCommand extends AbstractBaseCommand {

    @Parameter(names = {"-l", "--long"}, description = "show details")
    boolean listLongInfo = false;

    @Override
    public void doWork() throws CommandException {
        if (listLongInfo) {
            String format = "%-55s%10s%15s%n";
            System.out.printf(format, "Dir", "files", "Size");
            base.list()
                    .forEach(
                            path -> System.out.printf(
                                    format,
                                    path.toString(),
                                    base.getFilesCount(path),
                                    humanReadableSize(base.getTotalByteCount(path))
                            )
                    );
            System.out.printf(
                    format,
                    "Total:",
                    base.getFilesCount(),
                    humanReadableSize(base.getTotalByteCount())
            );
        } else {
            base.list().forEach(System.out::println);
        }
    }

    private String humanReadableSize(long size) {
        if (size > 1024 * 1024 * 1024 * 1024L) {
            return String.format("%.1fT", size * 1.0 / (1024 * 1024 * 1024 * 1024L));
        } else if (size > 1024 * 1024 * 1024L) {
            return String.format("%.1fG", size * 1.0 / (1024 * 1024 * 1024L));
        } else if (size > 1024 * 1024L) {
            return String.format("%.1fM", size * 1.0 / (1024 * 1024L));
        } else if (size > 1024L) {
            return String.format("%.1fK", size * 1.0 / 1024L);
        } else {
            return String.format("%d", size);
        }
    }
}
