package fr.cnrs.lacito.liftapi;

public class WrittingLiftDocumentException extends Exception {

    public WrittingLiftDocumentException(String string) {
        super(string);
    }

    public WrittingLiftDocumentException(Exception e) {
        super(e);
    }
}
