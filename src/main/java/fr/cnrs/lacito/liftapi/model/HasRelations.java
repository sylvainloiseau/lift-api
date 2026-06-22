package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasRelations permits LiftVariant, LiftSense, LiftEntry {
    public void addRelation(LiftRelation relation);
    public List<LiftRelation> getRelations();
}
