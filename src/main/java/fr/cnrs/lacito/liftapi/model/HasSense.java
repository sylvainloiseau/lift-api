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
     * Adds a sense at a given position.
     *
     * @param index where to insert the sense.
     * @param sense the sense to add.
     */
    public void addSense(int index, LiftSense sense);

    /**
     * Removes a sense from this component, and from the dictionary if this component
     * belongs to one.
     *
     * @param sense a sense of this component.
     */
    public void deleteSense(LiftSense sense);

    /**
     * Returns the list of senses of this component.
     *
     * @return the list of senses.
     */
    public List<LiftSense> getSenses();
}
