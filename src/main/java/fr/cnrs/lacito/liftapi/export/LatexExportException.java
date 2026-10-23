package fr.cnrs.lacito.liftapi.export;

/** The LaTeX export, its settings, or the compilation to PDF failed. The message is for the user. */
public final class LatexExportException extends Exception {

    public LatexExportException(String message) {
        super(message);
    }

    public LatexExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
