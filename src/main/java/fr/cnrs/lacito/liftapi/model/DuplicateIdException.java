package fr.cnrs.lacito.liftapi.model;

public final class DuplicateIdException extends IllegalStateException {
    public DuplicateIdException(String msg) {
        super(msg);
    }
}
