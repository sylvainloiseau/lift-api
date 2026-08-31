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
     * Returns the list of reversals of this component.
     *
     * @return the list of reversals.
     */
    public List<LiftReversal> getReversals();
}
