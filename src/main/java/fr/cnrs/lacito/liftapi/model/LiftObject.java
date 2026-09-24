package fr.cnrs.lacito.liftapi.model;

/**
 * The root of both the concrete classes and the interfaces for all Lift dictionary components.
 */
public sealed interface LiftObject permits HasReversal, HasTrait, HasNote, HasSense, HasPronunciation, HasField, HasAnnotation, HasRelations, AbstractLiftRoot {

}
