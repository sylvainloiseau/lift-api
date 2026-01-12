package fr.cnrs.lacito.liftapi.model;

public final class DuplicateTypeException
    extends IllegalStateException {
    DuplicateTypeException(String msg) {
        super(msg);
    }
}
