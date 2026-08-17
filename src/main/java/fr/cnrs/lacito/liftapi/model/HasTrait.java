package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasTrait extends LiftObject
    permits AbstractExtensibleWithoutField, GrammaticalInfo
{
    public void addTrait(LiftTrait t);
    public List<LiftTrait> getTraits();
}
