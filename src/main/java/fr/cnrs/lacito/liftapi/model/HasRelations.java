package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftRelation}.
 */
public sealed interface HasRelations extends LiftObject permits LiftVariant, LiftSense, LiftEntry {
    public void addRelation(LiftRelation relation);
    public List<LiftRelation> getRelations();
}
