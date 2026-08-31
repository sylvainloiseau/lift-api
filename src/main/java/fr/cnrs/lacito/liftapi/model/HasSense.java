package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftSense}.
 */
public sealed interface HasSense extends LiftObject permits LiftEntry, LiftSense {

    /**
     * Adds a sense to this component.
     *
     * @param sense the sense to add.
     */
    public void addSense(LiftSense sense);

    /**
     * Returns the list of senses of this component.
     *
     * @return the list of senses.
     */
    public List<LiftSense> getSenses();
}
