package fr.cnrs.lacito.liftapi.model;

/**
 * Encountered when two {@link HasType} objects, in the same component,
 * (for instances, two {@link LiftNote}s on the same {@link LiftEntry}),
 * have the same type.
 */
public final class DuplicateTypeException
    extends IllegalStateException {
    DuplicateTypeException(String msg) {
        super(msg);
    }
}
