package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftSense}.
 */
public sealed interface HasSense extends LiftObject permits LiftEntry, LiftSense {
    public void addSense(LiftSense sense);
    public List<LiftSense> getSenses();
}
