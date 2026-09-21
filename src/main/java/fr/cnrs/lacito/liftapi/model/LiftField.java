package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.SimpleObjectProperty;

/**
 * A Lift field (not to be confused with {@link LiftTrait}, {@link LiftAnnotation}, {@link LiftNote}; for comparison see {@link LiftTrait}).
 *
 * A field is a generalised element to allow an application to store information in a LIFT file that
 * isn't explicitly described in the LIFT standard. Fields are described as part of the header
 * information so that applications can give some descriptive meaning to the information they add
 * to a file. (Lift specification, p. 13)
 *
 * {@see HasField}.
 */
public final class LiftField extends AbstractExtensibleWithoutField {

    protected AbstractExtensibleWithField parent;

    private final SimpleObjectProperty<LiftFieldAndTraitDefinition> specification;

    public LiftField(LiftFieldAndTraitDefinition name) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        this.specification = new SimpleObjectProperty<>(
            this,
            "name",
            name
        );
    }

    // --------------------------------------------------------
    // Specification
    // --------------------------------------------------------

    public LiftFieldAndTraitDefinition getSpecification() {
        return this.specification.get();
    }

    public SimpleObjectProperty<LiftFieldAndTraitDefinition> specificationProperty() {
        return this.specification;
    }

    // --------------------------------------------------------
    // Parent
    // --------------------------------------------------------

    public AbstractExtensibleWithField getParent() {
        return this.parent;
    }

    /**
     * @param parent the new parent, or {@code null} when detaching this field
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(AbstractExtensibleWithField parent) {
        this.parent = parent;
    }

    // --------------------------------------------------------
    // Text
    // --------------------------------------------------------

    public MultiText getText() {
        return getMainMultiText();
    }

    public void addText(Form form) {
        getMainMultiText().add(form);
    }

    // --------------------------------------------------------
    // Static helper
    // --------------------------------------------------------

    public static LiftField create(LiftFieldAndTraitDefinition name) {
        return new LiftField(name);
    }

}
