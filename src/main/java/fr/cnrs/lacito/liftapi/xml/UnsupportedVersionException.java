package fr.cnrs.lacito.liftapi.xml;

/**
 * Thrown when a document declares a LIFT version this library cannot read, or
 * declares none at all.
 *
 * Unchecked so that it can travel out of the SAX callbacks; the reader wraps it in
 * a {@link fr.cnrs.lacito.liftapi.LiftDocumentLoadingException}.
 */
public class UnsupportedVersionException extends IllegalArgumentException {

    public UnsupportedVersionException(String message) {
        super(message);
    }

}
