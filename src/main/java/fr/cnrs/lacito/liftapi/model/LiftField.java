package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;

/**
 * A field is a generalised element to allow an application to store information in a LIFT file that
isn't explicitly described in the LIFT standard. Fields are described as part of the header
information so that applications can give some descriptive meaning to the information they add
to a file. (Lift specification, p. 13)
 *
 * {@see HasField}.
 */
public final class LiftField extends AbstractExtensibleWithoutField {

    protected AbstractExtensibleWithField parent;

    private final SimpleObjectProperty<LiftFieldAndTraitDefinition> nameProperty;

    public LiftField(LiftFieldAndTraitDefinition name) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        this.nameProperty = new SimpleObjectProperty<>(
            this,
            "name",
            name
        );
    }

    public LiftFieldAndTraitDefinition getName() {
        return nameProperty.get();
    }

    public AbstractExtensibleWithField getParent() {
        return parent;
    }

    protected void setParent(AbstractExtensibleWithField parent) {
        if (parent == null) throw new IllegalArgumentException("Parent is null");
        this.parent = parent;
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    public SimpleObjectProperty<LiftFieldAndTraitDefinition> nameProperty() {
        return this.nameProperty;
    }

    public static LiftField create(LiftFieldAndTraitDefinition name) {
        return new LiftField(name);
    }

    public void addText(Form form) {
        getText().add(form);
    }
}
