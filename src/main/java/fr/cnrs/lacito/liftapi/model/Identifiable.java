package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have a LiftId.
 */
public sealed interface Identifiable
    permits AbstractIdentifiable {

    /**
     * Sets the id of this component.
     *
     * @param id the id to set.
     */
    abstract public void setId(String id);

    /**
     * Sets the guid of this component.
     *
     * @param guid the guid to set.
     */
    abstract public void setGuid(String guid);
}
