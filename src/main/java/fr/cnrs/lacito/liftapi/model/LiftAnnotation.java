package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
public final class LiftAnnotation extends AbstractLiftRoot implements HasType {

    protected HasAnnotation parent;

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    private final StringProperty valueProperty = new SimpleStringProperty(this, "value", "");;
    private final StringProperty whoProperty = new SimpleStringProperty(this, "who", "");
    private final StringProperty whenProperty = new SimpleStringProperty(this, "when", "");


    public LiftAnnotation() {

    }

    public LiftAnnotation(LiftHeaderRangeElement element) {
        typeProperty.set(element);
    }

    protected void setParent(HasAnnotation parent) {
        this.parent = parent;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    @Override
    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
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

    public ObjectProperty<LiftHeaderRangeElement> nameProperty() {
        return typeProperty;
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

    public void setName(LiftHeaderRangeElement e) {
        typeProperty.set(e);
    }
}
