package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftReversal}.
 */
public sealed interface HasReversal extends LiftObject permits LiftSense, LiftReversal {

    /**
     * Adds a reversal to this component.
     *
     * @param reversal the reversal to add.
     */
    public void addReversal(LiftReversal reversal);

    /**
     * Removes a reversal from this component, and from the dictionary if this component
     * belongs to one.
     *
     * @param reversal a reversal of this component.
     */
    public void deleteReversal(LiftReversal reversal);

    /**
     * Changes the type of a reversal of this component. {@link LiftReversal#setType(Feature)}
     * calls this when the reversal has a parent.
     *
     * @param reversal a reversal of this component.
     * @param type the new type.
     */
    public void retypeReversal(LiftReversal reversal, Feature type);

    /**
     * Returns the list of reversals of this component.
     *
     * @return the list of reversals.
     */
    public List<LiftReversal> getReversals();
}
