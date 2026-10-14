package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;

/**
 * A Lift annotation (not to be confused with {@link LiftTrait}, {@link LiftField}, {@link LiftNote}; for comparison see {@link LiftTrait}).
 * 
 * Annotations can appear on most lift objects, including
 * {@link LiftTrait}, {@link LiftField} or in the {@link Form}s of a {@link MultiText} object.
 *
 * Since an annotation can itself contains a MultiText object,
 * there is an possibility of unlimited recursive hierarchy of {@link Form} and {@link LiftAnnotation}.
 *
 * Lift components that can have {@see LiftAnnotation} implement the interface {@see HasAnnotation}.
 */
public final class LiftAnnotation extends AbstractLiftRoot implements HasType {

    protected HasAnnotation parent;

    private final TrackedProperty<Feature> typeProperty =
        new TrackedProperty<>(this, ModelProperty.TYPE, null);
    private final StringProperty valueProperty =
        new TrackedStringProperty(this, ModelProperty.ANNOTATION_VALUE, "");
    private final StringProperty whoProperty =
        new TrackedStringProperty(this, ModelProperty.ANNOTATION_WHO, "");
    private final StringProperty whenProperty =
        new TrackedStringProperty(this, ModelProperty.ANNOTATION_WHEN, "");


    public LiftAnnotation() {

    }

    public LiftAnnotation(Feature element) {
        typeProperty.set(element);
    }

    /**
     * Protected: this method is called by the parent when it adopts this annotation.
     * 
     * @param parent
     */
    protected void setParent(HasAnnotation parent) {
        this.parent = parent;
    }

    public HasAnnotation getParent() {
        return parent;
    }

    /** The component, multitext or form this annotation is on. */
    @Override
    Object holder() {
        return parent;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    // Type -----------------------------------

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Change the type of this annotation, through
     * {@link HasAnnotation#retypeAnnotation(LiftAnnotation, Feature)} when it has a
     * parent.
     */
    @Override
    public void setType(Feature type) {
        if (parent != null) {
            parent.retypeAnnotation(this, type);
        } else {
            assignType(type);
        }
    }

    /**
     * Write the type. Called by {@link #setType(Feature)} when this component has no
     * parent, and otherwise by the parent's {@code retype} method once it has checked
     * and re-keyed what depends on the type.
     */
    void assignType(Feature type) {
        typeProperty.set(type);
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    // Value -----------------------------------

    public String getValue() {
        return valueProperty.get();
    }

    public void setValue(String value) {
        String v = value == null ? "" : value.trim();
        this.valueProperty.set(v);
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    // Who -----------------------------------

    public String getWho() {
        return whoProperty.get();
    }

    public void setWho(String who) {
        String v = who == null ? "" : who.trim();
        this.whoProperty.set(v);
    }

    public StringProperty whoProperty() {
        return whoProperty;
    }

    // When -----------------------------------

    public String getWhen() {
        return whenProperty.get();
    }

    public void setWhen(String when) {
        String v = when == null ? "" : when.trim();
        this.whenProperty.set(v);
    }

    public StringProperty whenProperty() {
        return whenProperty;
    }


    /**
     * An annotation can hang off a component, but also off a {@link MultiText} or a
     * {@link Form}, neither of which is an {@code AbstractLiftRoot}. In the
     * {@code MultiText} case the chain continues through the text's own owner, and in
     * the {@code Form} case through the multitext holding the form: so form-level
     * annotations are registered like the others.
     */
    @Override
    public AbstractLiftRoot getParentNode() {
        return switch (parent) {
            case AbstractLiftRoot r -> r;
            case MultiText m -> m.getParent();
            case Form f -> f.getMultiText() == null ? null : f.getMultiText().getParent();
            case null, default -> null;
        };
    }
}
