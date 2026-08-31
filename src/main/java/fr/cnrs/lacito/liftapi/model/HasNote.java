package fr.cnrs.lacito.liftapi.model;

import java.util.Map;

/**
 * Interface for LIFT components that can have {@link LiftNote}.
 */
public sealed interface HasNote
    extends LiftObject
    permits AbstractNotable {

    /**
     * Add a note to the Lift object.
     *
     * @param note the {@link LiftNote}
     * @throws DuplicateTypeException if the receiver already contains a note of the same type.
     */
    public void addNote(LiftNote note) throws DuplicateTypeException;

    /**
     * Returns the note of the given type.
     *
     * @param type the type of the note to return.
     * @return the note of the given type
     * @throws IllegalArgumentException if the note of the given type does not exist.
     */
    public LiftNote getNote(String type);

    /**
     * Returns the notes of this component.
     *
     * @return the notes.
     */
    public Map<String, LiftNote> getNotes();

}
