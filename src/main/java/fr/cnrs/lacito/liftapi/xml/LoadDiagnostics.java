package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LoadDiagnostic;
import fr.cnrs.lacito.liftapi.LoadDiagnostic.Kind;
import fr.cnrs.lacito.liftapi.LoadOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What the reader noticed while loading a file, collected into
 * {@link LoadDiagnostic}s: the one channel for everything it skips, keeps without saving,
 * or finds undeclared. Nothing the reader notices goes only to the logger.
 *
 * <p>The same problem met again is counted, not listed again: a diagnostic is keyed by its
 * kind and its subject (an element name, an attribute, a range...), keeps the position and
 * message of its first occurrence, and is logged once. A file with a foreign element in
 * each entry gives one diagnostic and one log line.
 *
 * <p>Past {@link LoadOptions#maxDiagnostics()} distinct diagnostics, new ones are only
 * counted, in a final {@link Kind#DIAGNOSTICS_TRUNCATED} one; whether saving loses data is
 * still tracked over all of them.
 *
 * <p>In a strict load, the first diagnostic whose kind
 * {@linkplain Kind#losesDataOnSave() loses data on save} fails the load: an exception is
 * thrown from {@link #report}, which the SAX handler gives the position of the element.
 *
 * <p>Nothing here runs while a clean file is read: the cost is on the exceptional path only.
 */
final class LoadDiagnostics {

    private static final Logger LOGGER = Logger.getLogger(LoadDiagnostics.class.getName());

    /** A diagnostic being collected: the first occurrence, and a count. */
    private static final class Pending {
        final Kind kind;
        final String message;
        final Path file;
        final int line;
        final int column;
        final String subject;
        int occurrences = 1;

        Pending(Kind kind, String message, Path file, int line, int column, String subject) {
            this.kind = kind;
            this.message = message;
            this.file = file;
            this.line = line;
            this.column = column;
            this.subject = subject;
        }

        LoadDiagnostic toDiagnostic() {
            return new LoadDiagnostic(kind, kind.severity(), message, file, line, column, subject, occurrences);
        }
    }

    private final boolean strict;
    private final int max;

    /** By kind and subject, in order of first occurrence. */
    private final Map<String, Pending> pending = new LinkedHashMap<>();

    /** The occurrences of the diagnostics not listed, past the limit. */
    private int truncated;

    private boolean losesDataOnSave;

    LoadDiagnostics(LoadOptions options) {
        this.strict = options.strict();
        this.max = options.maxDiagnostics();
    }

    /**
     * Reports a problem.
     *
     * @param kind what happened
     * @param subject what it is about (the key, with the kind, under which occurrences are
     *        counted); {@code null} to key it by its message
     * @param message an English message, giving the line
     * @param file the file read at that point, or {@code null}
     * @param line the line, or -1
     * @param column the column, or -1
     * @throws IllegalStateException in a strict load, if the kind loses data on save
     */
    void report(Kind kind, String subject, String message, Path file, int line, int column) {
        if (kind.losesDataOnSave()) {
            if (strict) {
                throw new IllegalStateException(message + " (strict load)");
            }
            losesDataOnSave = true;
        }
        String key = kind.name() + '\u0000' + (subject != null ? subject : message);
        Pending known = pending.get(key);
        if (known != null) {
            known.occurrences++;
            return;
        }
        if (pending.size() >= max) {
            truncated++;
            return;
        }
        pending.put(key, new Pending(kind, message, file, line, column, subject));
        LOGGER.log(switch (kind.severity()) {
            case INFO -> Level.INFO;
            case WARNING -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        }, message);
    }

    /** Whether something reported loses data on save, listed or not. */
    boolean losesDataOnSave() {
        return losesDataOnSave;
    }

    /** The diagnostics, in order of first occurrence. */
    List<LoadDiagnostic> toList() {
        List<LoadDiagnostic> list = new ArrayList<>(pending.size() + 1);
        for (Pending p : pending.values()) {
            list.add(p.toDiagnostic());
        }
        if (truncated > 0) {
            list.add(new LoadDiagnostic(Kind.DIAGNOSTICS_TRUNCATED, null,
                truncated + " more problems, not listed (limit of " + max + " diagnostics).",
                null, -1, -1, null, truncated));
        }
        return List.copyOf(list);
    }
}
