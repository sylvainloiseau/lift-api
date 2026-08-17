package fr.cnrs.lacito.liftapi.model;

public sealed interface HasType
    permits LiftNote, LiftEtymology, LiftReversal, LiftRelation
// LiftField, LiftTrait, LiftAnnotation, GramType
{
    LiftHeaderRangeElement getType();
    
}
