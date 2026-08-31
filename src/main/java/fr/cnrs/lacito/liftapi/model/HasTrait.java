package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftTrait}.
 */
public sealed interface HasTrait extends LiftObject
    permits AbstractExtensibleWithoutField, GrammaticalInfo
{
    /**
     * Adds a trait to this component.
     *
     * @param t the trait to add.
     */
    public void addTrait(LiftTrait t);

    /**
     * Returns the list of traits of this component.
     *
     * @return the list of traits.
     */
    public List<LiftTrait> getTraits();
}
