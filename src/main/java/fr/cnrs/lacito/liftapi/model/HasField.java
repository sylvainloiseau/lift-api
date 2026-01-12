package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for lift objects that can receive fields.
 */
public sealed interface HasField
    extends ExtensibleWithoutField
    permits AbstractExtensibleWithField {

    /**
     * Add a field to this object.
     *
     * @param f the field.
     * @throws DuplicateTypeException
     */
    public void addField(LiftField f) throws DuplicateTypeException;

    public LiftField getField(String type);

    public List<LiftField> getFields();
}
