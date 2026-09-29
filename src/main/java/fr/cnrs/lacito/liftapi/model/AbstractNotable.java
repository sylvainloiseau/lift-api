package fr.cnrs.lacito.liftapi.model;

import java.util.Map;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

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
    protected final MapProperty<Feature, LiftNote> notesProperty =
        new SimpleMapProperty<>(
            this,
            "notes",
            FXCollections.observableHashMap()
        );

    @Override
    public void addNote(LiftNote n) throws DuplicateTypeException {
        Feature type = n.getType();
        if (notesProperty.containsKey(type)) {
            throw new DuplicateTypeException(
                "Duplicate note type '" +
                    type.getId() +
                    "' on " +
                    describe() +
                    "; existing note types: " +
                    noteTypeIds()
            );
        }
        notesProperty.put(type, n);
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
        return notesProperty.keySet().stream().map(Feature::getId).toList();
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
        for (Map.Entry<Feature, LiftNote> e : notesProperty.entrySet()) {
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
        if (!notesProperty.containsKey(type)) {
            throw new IllegalArgumentException(
                "Not note with type: " + (type == null ? null : type.getId()) + "."
            );
        }
        return notesProperty.get(type);
    }

    @Override
    public Map<Feature, LiftNote> getNotes() {
        return notesProperty.get();
    }

    public MapProperty<Feature, LiftNote> notesProperty() {
        return notesProperty;
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
        requireChild(note, note != null && notesProperty.get(note.getType()) == note);
        if (type == null) throw new IllegalArgumentException("note type cannot be null");
        Feature old = note.getType();
        if (type == old) return;
        if (notesProperty.containsKey(type)) {
            throw new DuplicateTypeException(
                "Duplicate note type '" +
                    type.getId() +
                    "' on " +
                    describe() +
                    "; existing note types: " +
                    noteTypeIds()
            );
        }
        notesProperty.remove(old);
        note.assignType(type);
        notesProperty.put(type, note);
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
            note != null && notesProperty.get(note.getType()) == note
        );
        orphaned(note);
        note.detach();
    }
}
