package fr.cnrs.lacito.liftapi.model;

public sealed interface HasRelations permits LiftVariant, LiftSense, LiftEntry {
    public void addRelation (LiftRelation relation);
}
