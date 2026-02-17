package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Setter;

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
public final class LiftAnnotation
    extends AbstractLiftRoot {

    protected final String name;
    protected Optional<String> value = Optional.empty();
    protected Optional<String> who = Optional.empty();
    protected Optional<String> when = Optional.empty();
    @Setter protected HasAnnotation parent;

    private final ReadOnlyStringWrapper namePropertyWrapper;
    private final StringProperty valueProperty;
    private final StringProperty whoProperty;
    private final StringProperty whenProperty;

    /**
     * Create an annotation. The name is the only mandatory component of an annotation.
     */
    protected LiftAnnotation(String name) {
        this.name = name;
        this.namePropertyWrapper = new ReadOnlyStringWrapper(this, "name", name);
        this.valueProperty = new SimpleStringProperty(this, "value", "");
        this.whoProperty = new SimpleStringProperty(this, "who", "");
        this.whenProperty = new SimpleStringProperty(this, "when", "");
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    public String getName() {
        return name;
    }

    public Optional<String> getValue() {
        return value;
    }

    public Optional<String> getWho() {
        return who;
    }

    public Optional<String> getWhen() {
        return when;
    }

    public HasAnnotation getParent() {
        return parent;
    }

    protected void setValue(String value) {
        this.value = Optional.of(value);
        this.valueProperty.set(value);
    }

    protected void setWho(String who) {
        this.who = Optional.of(who);
        this.whoProperty.set(who);
    }

    protected void setWhen(String when) {
        this.when = Optional.of(when);
        this.whenProperty.set(when);
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
}
