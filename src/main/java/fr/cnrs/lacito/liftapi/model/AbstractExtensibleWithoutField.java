package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Superclass of components that can have {@link LiftAnnotation} and {@link LiftTrait}.
 */
public abstract sealed class AbstractExtensibleWithoutField
    extends AbstractLiftRoot
    implements ExtensibleWithoutField, HasAnnotation, HasTrait
    permits AbstractExtensibleWithField, LiftField
{

    // The dates live in plain fields until a property is asked for: most components
    // have them, and few are ever bound to a UI.
    private String dateCreated;
    private TrackedProperty<String> dateCreatedProperty;
    private String dateModified;
    private TrackedProperty<String> dateModifiedProperty;

    final ObservableList<LiftAnnotation> annotations = FXCollections.observableArrayList();
    private ObservableList<LiftAnnotation> annotationsView;
    final ObservableList<LiftTrait> traits = FXCollections.observableArrayList();
    private ObservableList<LiftTrait> traitsView;

    @Override
    public void addTrait(LiftTrait t) {
        addTrait(traits.size(), t);
    }

    /**
     * Add a trait at a given position among the traits of this component.
     *
     * @param index where to insert the trait
     * @param t the trait to add
     */
    @Override
    public void addTrait(int index, LiftTrait t) {
        ExternalDefinitionGuard.requireWritable(this);
        traits.add(index, t);
        t.setParent(this);
        adopted(t);
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        addAnnotation(annotations.size(), a);
    }

    /**
     * Add an annotation at a given position among the annotations of this component.
     *
     * @param index where to insert the annotation
     * @param a the annotation to add
     */
    @Override
    public void addAnnotation(int index, LiftAnnotation a) {
        ExternalDefinitionGuard.requireWritable(this);
        annotations.add(index, a);
        a.setParent(this);
        adopted(a);
    }

    /**
     * @param value the creation date, or {@code null} for none
     */
    public void setDateCreated(String value) {
        ExternalDefinitionGuard.requireWritable(this);
        if (dateCreatedProperty != null) {
            dateCreatedProperty.set(value);
        } else {
            String old = dateCreated;
            dateCreated = value;
            Changes.valueChanged(this, ModelProperty.DATE_CREATED, old, value);
        }
    }

    /**
     * @param value the modification date, or {@code null} for none
     */
    public void setDateModified(String value) {
        ExternalDefinitionGuard.requireWritable(this);
        if (dateModifiedProperty != null) {
            dateModifiedProperty.set(value);
        } else {
            String old = dateModified;
            dateModified = value;
            Changes.valueChanged(this, ModelProperty.DATE_MODIFIED, old, value);
        }
    }

    public Optional<String> getDateCreated() {
        return Optional.ofNullable(
            dateCreatedProperty != null ? dateCreatedProperty.get() : dateCreated
        );
    }

    public Optional<String> getDateModified() {
        return Optional.ofNullable(
            dateModifiedProperty != null ? dateModifiedProperty.get() : dateModified
        );
    }

    /**
     * The creation date, as a property; {@code null} when there is none.
     */
    public ObjectProperty<String> dateCreatedProperty() {
        if (dateCreatedProperty == null) {
            dateCreatedProperty = new TrackedProperty<>(this, ModelProperty.DATE_CREATED, dateCreated);
            dateCreated = null;
        }
        return dateCreatedProperty;
    }

    /**
     * The modification date, as a property; {@code null} when there is none.
     */
    public ObjectProperty<String> dateModifiedProperty() {
        if (dateModifiedProperty == null) {
            dateModifiedProperty = new TrackedProperty<>(this, ModelProperty.DATE_MODIFIED, dateModified);
            dateModified = null;
        }
        return dateModifiedProperty;
    }

    /**
     * @return the annotations, as a read-only observable list; change them with
     *         {@link #addAnnotation} and {@link #deleteAnnotation}
     */
    @Override
    public ObservableList<LiftAnnotation> getAnnotations() {
        if (annotationsView == null) {
            annotationsView = FXCollections.unmodifiableObservableList(annotations);
        }
        return annotationsView;
    }

    /**
     * @return the traits, as a read-only observable list; change them with
     *         {@link #addTrait} and {@link #deleteTrait}
     */
    @Override
    public ObservableList<LiftTrait> getTraits() {
        if (traitsView == null) {
            traitsView = FXCollections.unmodifiableObservableList(traits);
        }
        return traitsView;
    }

    /**
     * @deprecated use {@link #getAnnotations()}, which is observable; this returns a
     *             read-only property over it, and will be removed
     */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftAnnotation> annotationsProperty() {
        return new ReadOnlyListWrapper<>(this, "annotations", getAnnotations()).getReadOnlyProperty();
    }

    /**
     * @deprecated use {@link #getTraits()}, which is observable; this returns a
     *             read-only property over it, and will be removed
     */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftTrait> traitsProperty() {
        return new ReadOnlyListWrapper<>(this, "traits", getTraits()).getReadOnlyProperty();
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a trait from this component, unregistering it if this component belongs to
     * a dictionary.
     *
     * @param t a trait of this component
     */
    @Override
    public void deleteTrait(LiftTrait t) {
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(t, traits.contains(t));
        removeChild(t);
    }

    /**
     * Remove an annotation from this component, unregistering it if this component
     * belongs to a dictionary.
     *
     * @param a an annotation of this component
     */
    @Override
    public void deleteAnnotation(LiftAnnotation a) {
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(a, annotations.contains(a));
        removeChild(a);
    }

    @Override
    public void retypeAnnotation(LiftAnnotation a, Feature type) {
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(a, annotations.contains(a));
        a.assignType(type);
    }
}
