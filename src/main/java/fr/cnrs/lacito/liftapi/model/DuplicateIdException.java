package fr.cnrs.lacito.liftapi.model;

public final class DuplicateIdException extends IllegalStateException {
    DuplicateIdException(String msg) {
        super(msg);
    }
}
