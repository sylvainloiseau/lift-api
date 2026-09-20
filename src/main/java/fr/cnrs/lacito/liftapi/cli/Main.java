package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import java.io.File;
import java.util.Comparator;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lift-api")
public final class Main implements Runnable {

    @Parameters(index = "0", arity = "1", description = "Path to a LIFT dictionary")
    private File dictionaryFile;

    public static void main(String[] args) {
        Main command = new Main();
        try {
            new CommandLine(command).parseArgs(args);
            command.run();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            LiftDictionary dictionary = LiftDictionary.loadDictionaryFromFile(
                dictionaryFile
            );
            String metaLanguages = dictionary.getMetaLanguageManager()
                .getLanguages()
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
            String objectLanguages = dictionary.getObjectLanguageManager()
                .getLanguages()
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));

            System.out.println(
                "Entries: " + dictionary.getLiftDictionaryRegistry().nEntries()
            );
            System.out.println("Meta languages: " + metaLanguages);
            System.out.println("Object languages: " + objectLanguages);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}