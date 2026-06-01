package fr.cnrs.lacito.liftapi;

public class WritingLiftDocumentException extends Exception {

    public WritingLiftDocumentException(String string) {
        super(string);
    }

    public WritingLiftDocumentException(Exception e) {
        super(e);
    }
}
