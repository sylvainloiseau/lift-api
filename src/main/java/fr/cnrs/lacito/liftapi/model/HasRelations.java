package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftRelation}.
 */
public sealed interface HasRelations extends LiftObject permits LiftVariant, LiftSense, LiftEntry {

    /**
     * Adds a relation to this component.
     *
     * @param relation the relation to add.
     */
    public void addRelation(LiftRelation relation);

    /**
     * Returns the list of relations of this component.
     *
     * @return the list of relations.
     */
    public List<LiftRelation> getRelations();
}
