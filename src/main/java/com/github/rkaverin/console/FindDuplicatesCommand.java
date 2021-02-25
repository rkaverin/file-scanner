package com.github.rkaverin.console;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Parameters(commandNames = "dupes", commandDescription = "find duplicates and new files")
public final class FindDuplicatesCommand extends AbstractThreadedCommand {

    @Parameter(names = {"-n", "--new"}, description = "show new files")
    boolean showNewFiles = false;
    @Parameter(names = {"-p", "--present"}, description = "show present files")
    boolean showPresentFiles = false;

    private final List<Path> newFiles = new ArrayList<>();
    private final Map<Path, List<Path>> duplicates = new HashMap<>();

    @Override
    protected void doWork() throws CommandException {
        try {
            base.duplicates(
                    dirPath,
                    executor,
                    getProgressBarCallback(),
                    newFiles,
                    duplicates
            );
        } catch (IOException | InterruptedException e) {
            throw new CommandException(e);
        }

        if (showNewFiles) {
            System.out.printf("New files found:%n");
            newFiles.forEach(System.out::println);
        }

        if (showPresentFiles) {
            System.out.println();
            System.out.printf("Duplicates found:%n");

            duplicates.forEach((path, list) -> {
                System.out.println(path);
                list.forEach(entry -> System.out.printf("\t%s%n", entry));
            });
        }
    }

}
