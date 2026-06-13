package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import lombok.Setter;

/**
 * A field is a generalised element to allow an application to store information in a LIFT file that
isn't explicitly described in the LIFT standard. Fields are described as part of the header
information so that applications can give some descriptive meaning to the information they add
to a file. (Lift specification, p. 13)
 * 
 * {@see HasField}.
 */
public final class LiftField
    extends AbstractExtensibleWithoutField {

    protected final String name;
    @Setter protected AbstractExtensibleWithField parent;

    private final ReadOnlyStringWrapper namePropertyWrapper;

    public LiftField(String name) {
        this.name = name;
        this.namePropertyWrapper = new ReadOnlyStringWrapper(this, "name", name);
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

    public static LiftField create(String name) {
        return new LiftField(name);
    }

    public void addText(Form form) {
        getText().add(form);
    }
}
