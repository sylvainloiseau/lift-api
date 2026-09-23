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

    protected final MapProperty<String, LiftNote> notesProperty =
        new SimpleMapProperty<>(
            this,
            "notes",
            FXCollections.observableHashMap()
        );

        // TODO : another problem of map whose keys can turn out of sync with
        // its values.
    @Override
    public void addNote(LiftNote n) throws DuplicateTypeException {
        Feature type = n.getType();
        if (notesProperty.containsKey(type.getId())) {
            throw new DuplicateTypeException(
                "Duplicate note type '" +
                    type.getId() +
                    "' on " +
                    describe() +
                    "; existing note types: " +
                    notesProperty.keySet()
            );
        }
        notesProperty.put(type.getId(), n);
        n.setParent(this);
        adopted(n);
    }

    /**
     * A short description of this component for diagnostics.
     *
     * Not every {@code AbstractNotable} is identifiable - {@link LiftExample} is not -
     * so this cannot simply cast to {@link AbstractIdentifiable}.
     */
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
        if (!notesProperty.containsKey(type)) {
            throw new IllegalArgumentException(
                "Not note with type: " + type + "."
            );
        }
        return notesProperty.get(type);
    }

    public Map<String, LiftNote> getNotes() {
        return notesProperty.get();
    }

    public MapProperty<String, LiftNote> notesProperty() {
        return notesProperty;
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
            note != null && notesProperty.get(note.getType().getId()) == note
        );
        orphaned(note);
        note.detach();
    }
}
