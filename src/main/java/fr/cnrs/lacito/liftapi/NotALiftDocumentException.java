package fr.cnrs.lacito.liftapi;

/// The file is well-formed XML, but not a LIFT document: its root element is not `<lift>`
/// in no namespace.
///
/// Raised at the first start tag, before anything is read: a large file that is not LIFT is
/// refused after its first bytes. A `.lift-ranges` file opened instead of the `.lift` file
/// that refers to it is told apart, in the message.
public final class NotALiftDocumentException extends LiftDocumentLoadingException {

    public NotALiftDocumentException(String message) {
        super(message);
    }
}
