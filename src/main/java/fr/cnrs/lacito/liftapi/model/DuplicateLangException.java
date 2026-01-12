package fr.cnrs.lacito.liftapi.model;

public final class DuplicateLangException extends IllegalStateException {
     
    DuplicateLangException(String msg) {
            super(msg);
    }
    
}
