package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftReversal}.
 */
public sealed interface HasReversal extends LiftObject permits LiftSense, LiftReversal {
    public void addReversal(LiftReversal reversal);
    public List<LiftReversal> getReversals();
}
