package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have a LiftId.
 */
public sealed interface Identifiable
    permits AbstractIdentifiable {
    
    abstract public void setId(String id);

    abstract public void setGuid(String guid);
}
