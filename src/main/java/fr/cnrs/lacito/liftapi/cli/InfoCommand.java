package fr.cnrs.lacito.liftapi.cli;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.LoadDiagnostic;
import fr.cnrs.lacito.liftapi.LoadOptions;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code lift-api info <file>}: what a dictionary holds, and what the reader noticed while
 * loading it. Also run by {@code lift-api <file>}.
 *
 * <p>The summary goes to the standard output; the load diagnostics, one line each, to the
 * standard error.
 */
@Command(name = "info",
    description = {
        "Print the number of entries, senses and examples, and the languages, of a LIFT dictionary,"
            + " then what the reader noticed while loading it: elements and attributes it skipped or"
            + " will not save, XML errors, declarations made twice, what the file uses without declaring it.",
        "Exit codes: 0 read; 1 cannot be read (not well-formed, not a LIFT document, a strict load failed);"
            + " 2 bad command line; 3 read, but saving it would lose part of the file."},
    footer = {
        "",
        "Examples:",
        "  lift-api info dict.lift",
        "  lift-api info --strict dict.lift      (fail on the first problem that would be lost on save)"})
final class InfoCommand implements Callable<Integer> {

    static final int EXIT_DATA_NOT_SAVED = 3;

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", arity = "1", description = "Path to a LIFT dictionary")
    private File dictionaryFile;

    @Option(names = "--strict", description = "Fail on the first problem that saving the dictionary would lose"
        + " (an unknown element or attribute, a declaration made twice, an XML error, a missing external range).")
    private boolean strict;

    @Override
    public Integer call() {
        LiftDictionary dictionary = Main.load(spec, dictionaryFile, LoadOptions.defaults().withStrict(strict));
        PrintWriter out = spec.commandLine().getOut();
        out.println("Entries: " + dictionary.getLiftDictionaryRegistry().nEntries());
        out.println("Senses: " + dictionary.getLiftDictionaryRegistry().getSenses().size());
        out.println("Examples: " + dictionary.getLiftDictionaryRegistry().getExamples().size());
        out.println("Meta languages: " + languages(dictionary.getMetaLanguageManager()));
        out.println("Object languages: " + languages(dictionary.getObjectLanguageManager()));
        out.flush();
        PrintWriter err = spec.commandLine().getErr();
        Path main = dictionaryFile.getAbsoluteFile().toPath();
        for (LoadDiagnostic d : dictionary.getLoadDiagnostics()) {
            err.println(describe(d, main));
        }
        err.flush();
        return dictionary.losesDataOnSave() ? EXIT_DATA_NOT_SAVED : 0;
    }

    /** "warning: message (12 times) [ranges file]". */
    static String describe(LoadDiagnostic d, Path main) {
        StringBuilder sb = new StringBuilder(d.severity().name().toLowerCase(Locale.ROOT))
            .append(": ").append(d.message());
        if (d.occurrences() > 1) {
            sb.append(" (").append(d.occurrences()).append(" times)");
        }
        d.getFile().filter(f -> !f.equals(main)).ifPresent(f -> sb.append(" [").append(f.getFileName()).append(']'));
        return sb.toString();
    }

    private static String languages(LiftDictionaryLanguagesManager m) {
        return m.getLanguages().stream()
            .sorted(Comparator.naturalOrder())
            .map(x -> x + " (" + m.getLanguageOccurrence(x) + ")")
            .collect(Collectors.joining(", "));
    }
}
