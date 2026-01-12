package fr.cnrs.lacito.liftapi.model;

/**
 * For objects that can have note.
 */
public sealed interface HasNote permits AbstractNotable {
    
    /**
     * Add a note to the Lift object.
     * 
     * @param note the {@link LiftNote}
     * @throws DuplicateTypeException if the receiver already contains a note of the same type.
     */
    public void addNote(LiftNote note) throws DuplicateTypeException ;

    public LiftNote getNote(String type);
}
