package com.github.rkaverin;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.github.rkaverin.commands.*;

public class Main {

    public static void main(String[] args) {

        JCommander jc = JCommander.newBuilder()
                .programName("file-scanner")
                .acceptUnknownOptions(false)
                .allowAbbreviatedOptions(true)
                .defaultProvider(optionName -> {
                    if ("-t".equals(optionName) || "--threads".equals(optionName)) {
                        return "1";
                    }
                    if ("-b".equals(optionName) || "--base".equals(optionName)) {
                        return "default.z";
                    }
                    return null;
                })
                .addCommand(new AddCommand())
                .addCommand(new ListCommand())
                .addCommand(new RemoveCommand())
                .addCommand(new PurgeCommand())
                .addCommand(new FindDuplicatesCommand())
                .addCommand(new UpdateCommand())
                .build();

        try {
            jc.parse(args);
            if (jc.getParsedCommand() == null) {
                jc.usage();
            } else {
                for (Object cmd : jc.getCommands()
                        .get(jc.getParsedCommand())
                        .getObjects()
                ) {
                    if (cmd instanceof Command) {
                        ((Command) cmd).execute();
                    }
                }
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            e.usage();
        } catch (CommandException e) {
            System.out.printf("Ошибка: %s%n", e.getMessage());
        }
    }
}
