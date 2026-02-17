package fr.cnrs.lacito.liftapi.model;

import java.util.Map;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

/**
 * Can receive Note (not to be confused with annotation).
 * 
 * @see LiftNote
 */
public abstract sealed class AbstractNotable
    extends AbstractExtensibleWithField
    implements HasNote
    permits AbstractIdentifiable, LiftExample {

    protected final MapProperty<String, LiftNote> notesProperty =
            new SimpleMapProperty<>(this, "notes", FXCollections.observableHashMap());

    @Override
    public void addNote(LiftNote n) throws DuplicateTypeException {
        String key;
        if (n.type.isEmpty()) {
            key = "";
        } else {
            key = n.type.get();
        }
        if (notesProperty.containsKey(key)) {
            throw new IllegalStateException("Duplicate Note type: " + key + "; Id: " + ((AbstractIdentifiable)this).getId());
        }
        notesProperty.put(key, n);
        n.setParent(this);
    }

    @Override
    public LiftNote getNote(String type) {
        if (!notesProperty.containsKey(type)) {
            throw new IllegalArgumentException("Not note with type: " + type  + ".");
        }
        return notesProperty.get(type);
    }

    public Map<String, LiftNote> getNotes() {
        return notesProperty.get();
    }

    public MapProperty<String, LiftNote> notesProperty() {
        return notesProperty;
    }
}
