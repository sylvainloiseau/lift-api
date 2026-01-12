package fr.cnrs.lacito.liftapi.model;

public sealed interface HasTrait
    permits AbstractExtensibleWithoutField, GrammaticalInfo {

    public void addTrait(LiftTrait t);

}
