package fr.cnrs.lacito.liftapi.model;

/**
 * LiftObject
 */
public sealed interface LiftObject permits HasTrait, HasNote, HasSense, HasPronunciation, HasField, HasAnnotation, HasRelations, AbstractLiftRoot {

}
