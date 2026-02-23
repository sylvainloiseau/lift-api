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
        this.valueProperty.addListener((obs, oldV, newV) -> {
            String v = newV == null ? "" : newV.trim();
            this.value = v.isEmpty() ? Optional.empty() : Optional.of(v);
        });
        this.whoProperty.addListener((obs, oldV, newV) -> {
            String v = newV == null ? "" : newV.trim();
            this.who = v.isEmpty() ? Optional.empty() : Optional.of(v);
        });
        this.whenProperty.addListener((obs, oldV, newV) -> {
            String v = newV == null ? "" : newV.trim();
            this.when = v.isEmpty() ? Optional.empty() : Optional.of(v);
        });
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
        String v = value == null ? "" : value.trim();
        this.value = v.isEmpty() ? Optional.empty() : Optional.of(v);
        this.valueProperty.set(v);
    }

    protected void setWho(String who) {
        String v = who == null ? "" : who.trim();
        this.who = v.isEmpty() ? Optional.empty() : Optional.of(v);
        this.whoProperty.set(v);
    }

    protected void setWhen(String when) {
        String v = when == null ? "" : when.trim();
        this.when = v.isEmpty() ? Optional.empty() : Optional.of(v);
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
}
