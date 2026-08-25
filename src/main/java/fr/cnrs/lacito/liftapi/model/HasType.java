package fr.cnrs.lacito.liftapi.model;

/**
 * Some dictionary components have a type, which is an element 
 * of a range: (a {@LiftHeaderRangeElement}).
 * 
 * The {@LiftField} and {@HasType} do not implement this interface: their type is a {@fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition}.
 */
public sealed interface HasType
    permits LiftNote, LiftEtymology, LiftReversal, LiftRelation, LiftVariant, LiftAnnotation
    // + Translation (not an object)
// LiftField, LiftTrait, GramType
{
    LiftHeaderRangeElement getType();
    
}
