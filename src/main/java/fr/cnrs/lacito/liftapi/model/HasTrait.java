package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasTrait
    permits AbstractExtensibleWithoutField, GrammaticalInfo
{
    public void addTrait(LiftTrait t);
    public List<LiftTrait> getTraits();
}
