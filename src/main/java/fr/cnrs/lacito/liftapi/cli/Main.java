package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
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
        // execute() maps a thrown exception and a bad command line to a non-zero
        // exit code. Catching and printing here instead reported every failure as
        // success.
        CommandLine command = new CommandLine(new Main());
        command.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
            // Report the problem, not a stack trace.
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
            return cmd.getCommandSpec().exitCodeOnExecutionException();
        });
        System.exit(command.execute(args));
    }

    @Override
    public void run() {
        LiftDictionary dictionary;
        try {
            dictionary = LiftDictionary.loadDictionaryFromFile(dictionaryFile);
        } catch (LiftDocumentLoadingException e) {
            throw new CommandLine.ExecutionException(
                new CommandLine(this),
                "Cannot read " + dictionaryFile + ": " + e.getMessage(),
                e
            );
        }
        LiftDictionaryLanguagesManager metaLanguageManager = dictionary.getMetaLanguageManager();
        String metaLanguages = dictionary.getMetaLanguageManager()
            .getLanguages()
            .stream()
            .sorted(Comparator.naturalOrder())
            .map(x -> x + " (" + metaLanguageManager.getLanguageOccurrence(x) + ")")
            .collect(Collectors.joining(", "));

        LiftDictionaryLanguagesManager objectLanguageManager = dictionary.getObjectLanguageManager();
        String objectLanguages = dictionary.getObjectLanguageManager()
            .getLanguages()
            .stream()
            .sorted(Comparator.naturalOrder())
            .map(x -> x + " (" + objectLanguageManager.getLanguageOccurrence(x) + ")")
            .collect(Collectors.joining(", "));

        System.out.println(
            "Entries: " + dictionary.getLiftDictionaryRegistry().nEntries()
        );
        System.out.println(
            "Senses: " + dictionary.getLiftDictionaryRegistry().getSenses().size()
        );
        System.out.println(
            "Examples: " + dictionary.getLiftDictionaryRegistry().getExamples().size()
        );
        System.out.println("Meta languages: " + metaLanguages);
        System.out.println("Object languages: " + objectLanguages);
    }
}
