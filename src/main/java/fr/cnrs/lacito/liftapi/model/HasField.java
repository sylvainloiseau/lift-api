package fr.cnrs.lacito.liftapi.model;

import java.util.Map;

/**
 * Interface for LIFT components that can receive {@link LiftField}}.
 */
public sealed interface HasField
    extends ExtensibleWithoutField, LiftObject
    permits AbstractExtensibleWithField {

    /**
     * Add a field to this object.
     *
     * @param f the field.
     * @throws DuplicateTypeException
     */
    public void addField(LiftField f) throws DuplicateTypeException;

    /**
     * Returns the field with the given type.
     *
     * @param type the type of the field.
     * @return the field.
     */
    public LiftField getField(String type);

    /**
     * Returns the fields of this object.
     *
     * @return the fields.
     */
    public Map<String, LiftField> getFields();
}
