package fr.cnrs.lacito.liftapi;

/**
 * A file cannot be loaded as a dictionary: it cannot be read, is not well-formed XML, is
 * not a LIFT document ({@link NotALiftDocumentException}), declares a version this library
 * does not read, or holds something the model refuses (a duplicate id...). The message
 * gives the line when the parser knows it.
 */
public sealed class LiftDocumentLoadingException extends Exception permits NotALiftDocumentException {

    public LiftDocumentLoadingException(String string) {
        super(string);
    }

    public LiftDocumentLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiftDocumentLoadingException(Throwable cause) {
        super(cause);
    }

}
