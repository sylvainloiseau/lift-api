package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * Interface for LIFT components that can have a reference towards other LIFT component.
 */
public sealed interface HasRefId permits LiftVariant, LiftRelation {

    /**
     * Returns the reference ID of this component.
     *
     * @return the reference ID.
     */
    public Optional<String> getRefId();

    /**
     * Returns the reference object of this component.
     *
     * @return the reference object.
     */
    public AbstractIdentifiable getRefObject();

    /**
     * Sets the reference object of this component.
     *
     * @param refObject the reference object to set.
     */
    public void setRefObject(AbstractIdentifiable refObject);
}
