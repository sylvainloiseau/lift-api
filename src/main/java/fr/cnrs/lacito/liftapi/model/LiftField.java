package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import lombok.Setter;

/**
 * A Field.
 * 
 * {@see HasField}.
 */
public final class LiftField
    extends AbstractExtensibleWithoutField {

    protected final String name;
    @Setter protected AbstractExtensibleWithField parent;

    private final ReadOnlyStringWrapper namePropertyWrapper;

    protected LiftField(String name) {
        this.name = name;
        this.namePropertyWrapper = new ReadOnlyStringWrapper(this, "name", name);
    }

    /**
     * Factory method for creating new LiftField instances.
     * Used by builders and other factory patterns.
     */
    public static LiftField create(String name) {
        return new LiftField(name);
    }

    public String getName() {
        return name;
    }

    public AbstractExtensibleWithField getParent() {
        return parent;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    public ReadOnlyStringProperty nameProperty() {
        return namePropertyWrapper.getReadOnlyProperty();
    }

    // TODO : getText should be plural if addText means add one form
    public void addText(Form form) {
        addToMainMultiText(form);
    }
}
