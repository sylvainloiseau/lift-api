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
     * Removes a trait from this component, and from the dictionary if this component
     * belongs to one.
     *
     * @param t a trait of this component.
     */
    public void deleteTrait(LiftTrait t);

    /**
     * Returns the list of traits of this component.
     *
     * @return the list of traits.
     */
    public List<LiftTrait> getTraits();
}
