package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import java.io.File;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code lift-api info <file>}: what a dictionary holds. Also run by {@code lift-api <file>}. */
@Command(name = "info", description = "Print the number of entries, senses and examples, and the languages, of a LIFT dictionary.")
final class InfoCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", arity = "1", description = "Path to a LIFT dictionary")
    private File dictionaryFile;

    @Override
    public Integer call() {
        LiftDictionary dictionary = Main.load(spec, dictionaryFile);
        PrintWriter out = spec.commandLine().getOut();
        out.println("Entries: " + dictionary.getLiftDictionaryRegistry().nEntries());
        out.println("Senses: " + dictionary.getLiftDictionaryRegistry().getSenses().size());
        out.println("Examples: " + dictionary.getLiftDictionaryRegistry().getExamples().size());
        out.println("Meta languages: " + languages(dictionary.getMetaLanguageManager()));
        out.println("Object languages: " + languages(dictionary.getObjectLanguageManager()));
        out.flush();
        return 0;
    }

    private static String languages(LiftDictionaryLanguagesManager m) {
        return m.getLanguages().stream()
            .sorted(Comparator.naturalOrder())
            .map(x -> x + " (" + m.getLanguageOccurrence(x) + ")")
            .collect(Collectors.joining(", "));
    }
}
