package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have a type. A type {@link Feature},
 * part of a {@link FeatureSet}.
 * 
 * Note: the {@link LiftField} and {@link LiftTrait} components do not implement this interface:
 * their type is a {@link fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition}.
 */
public sealed interface HasType
    permits LiftNote, LiftEtymology, LiftReversal, LiftRelation, LiftVariant, LiftAnnotation
    // + Translation (not an object)
// LiftField, LiftTrait, GramType
{
    Feature getType();

    void setType(Feature type);
    
}
