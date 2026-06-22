package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for lift objects that can receive annotations.
 *
 */
public sealed interface HasAnnotation
    permits AbstractExtensibleWithoutField, Form, LiftTrait, MultiText
{
    public void addAnnotation(LiftAnnotation a);
    public List<LiftAnnotation> getAnnotations();
}
