package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have a reference to an 
 * external files documenting a linguistic material (visual representation of a sense or sound recording of a pronunciation).
 */
public sealed interface HasExternalDocument
    permits LiftMedia, LiftIllustration
{
    public String getHref();

    public MultiText getLabel();
}
