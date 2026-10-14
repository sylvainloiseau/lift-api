package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT component that can receive annotations.
 *
 */
public sealed interface HasAnnotation
    extends LiftObject
    permits AbstractExtensibleWithoutField, Form, LiftTrait, MultiText
{
    public void addAnnotation(LiftAnnotation a);

    /**
     * Adds an annotation at a given position among the annotations of this object.
     *
     * @param index where to insert the annotation.
     * @param a the annotation to add.
     */
    public void addAnnotation(int index, LiftAnnotation a);

    /**
     * Removes an annotation from this object, and from the dictionary if this object
     * belongs to one.
     *
     * @param a an annotation of this object.
     */
    public void deleteAnnotation(LiftAnnotation a);

    /**
     * Changes the type of an annotation of this component. {@link LiftAnnotation#setType(Feature)}
     * calls this when the annotation has a parent.
     *
     * @param a an annotation of this component.
     * @param type the new type.
     */
    public void retypeAnnotation(LiftAnnotation a, Feature type);

    public List<LiftAnnotation> getAnnotations();
}
