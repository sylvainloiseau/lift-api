package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * An annotation.
 *
 * Annotations can appear on most lift objects, including
 * {@link LiftTrait}, {@link LiftField} or in the {@link Form}s of a {@link MultiText} object.
 *
 * Since an annotation can itself contains a MultiText object,
 * there is an possibility of unlimited recursive hierarchy of {@link Form} and {@link LiftAnnotation}.
 *
 * {@see HasAnnotation}.
 */
public final class LiftAnnotation extends AbstractLiftRoot {

    protected HasAnnotation parent;

    private final ReadOnlyStringWrapper namePropertyWrapper;
    private final StringProperty valueProperty = new SimpleStringProperty(this, "value", "");;
    private final StringProperty whoProperty = new SimpleStringProperty(this, "who", "");
    private final StringProperty whenProperty = new SimpleStringProperty(this, "when", "");

    /**
     * Create an annotation. The name is the only mandatory component of an annotation.
     */
    public LiftAnnotation(String name) {
        this.namePropertyWrapper = new ReadOnlyStringWrapper(
            this,
            "name",
            name
        );
    }

    protected void setParent(HasAnnotation parent) {
        this.parent = parent;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    public String getName() {
        return namePropertyWrapper.get();
    }

    public String getValue() {
        return valueProperty.get();
    }

    public String getWho() {
        return whoProperty.get();
    }

    public String getWhen() {
        return whenProperty.get();
    }

    public HasAnnotation getParent() {
        return parent;
    }

    public void setValue(String value) {
        String v = value == null ? "" : value.trim();
        this.valueProperty.set(v);
    }

    public void setWho(String who) {
        String v = who == null ? "" : who.trim();
        this.whoProperty.set(v);
    }

    public void setWhen(String when) {
        String v = when == null ? "" : when.trim();
        this.whenProperty.set(v);
    }

    public ReadOnlyStringProperty nameProperty() {
        return namePropertyWrapper.getReadOnlyProperty();
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public StringProperty whoProperty() {
        return whoProperty;
    }

    public StringProperty whenProperty() {
        return whenProperty;
    }

    public static LiftAnnotation create(String name, String value) {
        LiftAnnotation ann = new LiftAnnotation(name);
        ann.setValue(value);
        return ann;
    }

    public static LiftAnnotation create(String name) {
        return new LiftAnnotation(name);
    }
}
