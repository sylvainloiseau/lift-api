package fr.cnrs.lacito.liftapi.model;

import java.util.Map;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Superclass of component that can receive {@link LiftNote} (not to be confused with {@link LiftAnnotation}).
 *
 * @see LiftNote
 */
public abstract sealed class AbstractNotable
    extends AbstractExtensibleWithField
    implements HasNote
    permits AbstractIdentifiable, LiftExample
{

    /**
     * The notes, by type.
     *
     * Keyed by the {@link Feature} itself, not by its id: a feature compares by
     * identity, so renaming it ({@link FeatureSet#changeFeatureId}) leaves the keys
     * valid. A note's type only changes through {@link #retypeNote}, which re-keys it.
     */
    final ObservableMap<Feature, LiftNote> notes = FXCollections.observableHashMap();
    private ObservableMap<Feature, LiftNote> notesView;

    @Override
    public void addNote(LiftNote n) throws DuplicateTypeException {
        Feature type = n.getType();
        if (notes.containsKey(type)) {
            throw new DuplicateTypeException(
                "Duplicate note type '" +
                    type.getId() +
                    "' on " +
                    describe() +
                    "; existing note types: " +
                    noteTypeIds()
            );
        }
        notes.put(type, n);
        n.setParent(this);
        adopted(n);
    }

    /**
     * A short description of this component for diagnostics.
     *
     * Not every {@code AbstractNotable} is identifiable - {@link LiftExample} is not -
     * so this cannot simply cast to {@link AbstractIdentifiable}.
     */
    private java.util.List<String> noteTypeIds() {
        return notes.keySet().stream().map(Feature::getId).toList();
    }

    private String describe() {
        if (this instanceof AbstractIdentifiable identifiable) {
            return getClass().getSimpleName() +
                " '" +
                identifiable.getId().orElse("<no id>") +
                "'";
        }
        return getClass().getSimpleName();
    }

    @Override
    public LiftNote getNote(String type) {
        for (Map.Entry<Feature, LiftNote> e : notes.entrySet()) {
            if (e.getKey().getId().equals(type)) {
                return e.getValue();
            }
        }
        throw new IllegalArgumentException(
            "Not note with type: " + type + "."
        );
    }

    @Override
    public LiftNote getNote(Feature type) {
        if (!notes.containsKey(type)) {
            throw new IllegalArgumentException(
                "Not note with type: " + (type == null ? null : type.getId()) + "."
            );
        }
        return notes.get(type);
    }

    /**
     * @return the notes by type, as a read-only observable map; change them with
     *         {@link #addNote}, {@link #deleteNote} and {@link #retypeNote}
     */
    @Override
    public ObservableMap<Feature, LiftNote> getNotes() {
        if (notesView == null) {
            notesView = FXCollections.unmodifiableObservableMap(notes);
        }
        return notesView;
    }

    /**
     * @deprecated use {@link #getNotes()}, which is observable; this returns a read-only
     *             property over it, and will be removed
     */
    @Deprecated(forRemoval = true)
    public ReadOnlyMapProperty<Feature, LiftNote> notesProperty() {
        return new ReadOnlyMapWrapper<>(this, "notes", getNotes()).getReadOnlyProperty();
    }

    /**
     * Change the type of a note of this component, re-keying it.
     *
     * @param note a note of this component
     * @param type its new type
     * @throws DuplicateTypeException if this component already has another note of that
     *         type; nothing is changed then
     */
    @Override
    public void retypeNote(LiftNote note, Feature type) throws DuplicateTypeException {
        requireChild(note, note != null && notes.get(note.getType()) == note);
        if (type == null) throw new IllegalArgumentException("note type cannot be null");
        Feature old = note.getType();
        if (type == old) return;
        if (notes.containsKey(type)) {
            throw new DuplicateTypeException(
                "Duplicate note type '" +
                    type.getId() +
                    "' on " +
                    describe() +
                    "; existing note types: " +
                    noteTypeIds()
            );
        }
        notes.remove(old);
        notes.put(type, note);
        // Last, so that the change is reported once the note is under its new key.
        note.assignType(type);
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a note from this component, unregistering it if this component belongs to
     * a dictionary.
     *
     * @param note a note of this component
     */
    @Override
    public void deleteNote(LiftNote note) {
        requireChild(
            note,
            note != null && notes.get(note.getType()) == note
        );
        removeChild(note);
    }
}
