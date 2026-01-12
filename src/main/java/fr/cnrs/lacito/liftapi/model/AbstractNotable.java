package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Can receive Note (not to be confused with annotation).
 * 
 * @see LiftNote
 */
public abstract sealed class AbstractNotable
    extends AbstractExtensibleWithField
    implements HasNote
    permits AbstractIdentifiable, LiftExample {

    @Getter protected final Map<String, LiftNote> notes = new HashMap<>();

    @Override
    public void addNote(LiftNote n) throws DuplicateTypeException {
        String key;
        if (n.type.isEmpty()) {
            key = "";
        } else {
            key = n.type.get();
        }
        if (notes.containsKey(key)) {
            throw new IllegalStateException("Duplicate Note type: " + key + "; Id: " + ((AbstractIdentifiable)this).getId());
        }
        notes.put(key, n);
        n.setParent(this);
    }

    @Override
    public LiftNote getNote(String type) {
        if (!notes.containsKey(type)) {
            throw new IllegalArgumentException("Not note with type: " + type  + ".");
        }
        return notes.get(type);
    }

}
