package fr.cnrs.lacito.liftapi;

public final class LiftDocumentLoadingException extends Exception {

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
