package fr.cnrs.lacito.liftapi.export;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The result of {@link LatexExport#compile}.
 *
 * @param success whether the PDF was made
 * @param engine the engine that ran
 * @param pdf the PDF (it may be absent or stale when {@code success} is false)
 * @param log the LaTeX log
 * @param runs how many times the engine ran
 * @param error on failure, the first error of the log: its {@code file:line: message} or
 *        {@code ! message} line and the two lines after; null on success
 * @param errorLine the line of the .tex the error is on, when the log gives it
 */
public record LatexCompilation(
    boolean success,
    LatexEngine engine,
    Path pdf,
    Path log,
    int runs,
    String error,
    Integer errorLine
) {

    public Optional<String> firstError() {
        return Optional.ofNullable(error);
    }

    public Optional<Integer> firstErrorLine() {
        return Optional.ofNullable(errorLine);
    }
}
