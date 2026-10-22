package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

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

    /**
     * What the name of this field refers to. Unlike a trait's, it may change: the value of
     * a field is a text whatever its definition, so when the header declares the name of
     * an undeclared field, the field is rebound in place (see {@link LiftHeader}); the
     * name, by which the parent holds the field, never changes.
     */
    private final TrackedProperty<FieldSpecification> specification;

    /**
     * @param specification what the name of the field refers to: what
     *        {@link LiftHeader#resolveFieldSpecification(String)} returns for it
     */
    public LiftField(FieldSpecification specification) {
        if (specification == null) throw new IllegalArgumentException("The specification of a field cannot be null");
        this.specification = new TrackedProperty<>(this, ModelProperty.FIELD_SPECIFICATION, specification);
    }

    // --------------------------------------------------------
    // Specification
    // --------------------------------------------------------

    /**
     * What the name of this field refers to: a definition of the header, or an undeclared
     * name. See {@link FieldSpecification}.
     */
    public FieldSpecification getSpecification() {
        return this.specification.get();
    }

    /**
     * The specification of this field, read-only: it changes only when the header
     * declares or removes the definition of its name, and always keeps the same name (the
     * parent holds its fields by name).
     */
    public ReadOnlyObjectProperty<FieldSpecification> specificationProperty() {
        return this.specification.getReadOnlyProperty();
    }

    /** The name of this field, as written in its {@code type} (0.13: {@code tag}) attribute. */
    public String getName() {
        return this.specification.get().getName();
    }

    /**
     * Rebinds this field to another specification of the same name. Package-private: the
     * header calls it to keep the field's specification what its name resolves to.
     */
    void respecify(FieldSpecification newSpecification) {
        if (!newSpecification.getName().equals(getName())) {
            throw new IllegalArgumentException(
                "A field cannot change its name: " + getName() + " -> " + newSpecification.getName());
        }
        this.specification.set(newSpecification);
    }

    // --------------------------------------------------------
    // Parent
    // --------------------------------------------------------

    public AbstractExtensibleWithField getParent() {
        return this.parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftExample.
     * 
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

        ExternalDefinitionGuard.requireWritable(this);
        getMainMultiText().add(form);
    }

    // --------------------------------------------------------
    // Static helper
    // --------------------------------------------------------

    public static LiftField create(FieldSpecification specification) {
        return new LiftField(specification);
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
