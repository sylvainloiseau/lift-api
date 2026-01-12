package fr.cnrs.lacito.liftapi.model;

/**
 * Reference to an external resource which documents 
 * a stated material (sense or pronunciation).
 */
public sealed interface ExternalDocument
    permits LiftMedia, LiftIllustration {

    public String getHref();

    public MultiText getLabel();

}
