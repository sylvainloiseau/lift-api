package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for lift objects that can receive annotations.
 * 
 */
public sealed interface HasAnnotation
    permits AbstractExtensibleWithoutField, Form, LiftTrait {

    public void addAnnotation(LiftAnnotation a);

}
