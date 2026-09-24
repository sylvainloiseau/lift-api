package fr.cnrs.lacito.liftapi.model;

/**
 * Encountered when two {@link Form} have the same lang in a {@link MultiText}.
 */
public final class DuplicateLangException extends IllegalStateException {
     
    DuplicateLangException(String msg) {
            super(msg);
    }
    
}
