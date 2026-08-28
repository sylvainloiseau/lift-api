package fr.cnrs.lacito.liftapi.model;

/**
 * Encountered when two components have the same lift Id.
 */
public final class DuplicateIdException extends IllegalStateException {
    public DuplicateIdException(String msg) {
        super(msg);
    }
}
