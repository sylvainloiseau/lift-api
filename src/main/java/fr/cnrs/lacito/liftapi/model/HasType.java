package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * Interface for LIFT components that can have a type. A type is a {@link Feature},
 * part of a {@link FeatureSet}.
 *
 * <h2>Changing the type</h2>
 *
 * The type is only ever changed through {@link #setType(Feature)}, and
 * {@link #typeProperty()} is read-only so that it cannot be bypassed. When the component
 * has a parent, {@code setType} delegates to the parent's {@code retypeX} method
 * ({@link HasNote#retypeNote}, {@link LiftExample#retypeTranslation}, ...), which is
 * where whatever depends on the type is kept in step: a parent holding its children
 * keyed by type ({@link LiftNote}, {@link LiftTranslation}) refuses a duplicate and
 * re-keys the child before the type changes. A component without a parent simply takes
 * the new type.
 *
 * Note: the {@link LiftField} and {@link LiftTrait} components do not implement this interface:
 * their type is a {@link fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition}.
 */
public sealed interface HasType
    permits LiftNote, LiftEtymology, LiftReversal, LiftRelation, LiftVariant, LiftAnnotation,
        LiftTranslation
// LiftField, LiftTrait, GramType
{
    Feature getType();

    /**
     * Sets the type of this component, through its parent's {@code retypeX} method when
     * it has one.
     *
     * @param type the type to set.
     * @throws DuplicateTypeException if the parent holds its children by type and
     *         already has another one of that type; nothing is changed then
     */
    void setType(Feature type);

    /**
     * The type, as a read-only observable property.
     *
     * @return the type property
     */
    ReadOnlyObjectProperty<Feature> typeProperty();

}
