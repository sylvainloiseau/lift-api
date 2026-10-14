package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * Superclass of components that have Lift ID and Lift GUID.
 */
public abstract sealed class AbstractIdentifiable
    extends AbstractNotable
    implements Identifiable
    permits LiftEntry, LiftSense {

    // Plain fields until a property is asked for (see AbstractExtensibleWithoutField).
    private String id;
    private TrackedProperty<String> idProperty;
    private String guid;
    private TrackedProperty<String> guidProperty;

    /**
     * Set the LIFT id of this entry or sense.
     *
     * On a registered component, the dictionary's id indexes follow. The new id is
     * refused when another entry (or sense) already has it, and the change is refused
     * while other components refer to the current id (relations, variants), as a
     * deletion would be: they would be left pointing at nothing.
     *
     * @param id the new id; {@code null} clears it (only on an unregistered component)
     * @throws DuplicateIdException if the id is taken
     * @throws IllegalStateException if the current id is referred to
     */
    @Override
    public void setId(String id) {
        String old = getId().orElse(null);
        if (java.util.Objects.equals(old, id)) {
            return;
        }
        if (getUUID() != null) {
            LiftDictionary d = getOwningDictionary();
            if (d != null) {
                if (id == null) {
                    throw new IllegalArgumentException(
                        "A registered " + getClass().getSimpleName() + " must have an id.");
                }
                d.getMutator().changeLiftId(this, old, id);
            }
        }
        if (idProperty != null) {
            idProperty.set(id);
        } else {
            this.id = id;
            Changes.valueChanged(this, ModelProperty.LIFT_ID, old, id);
        }
    }

    /**
     * @param guid the guid, or {@code null} for none
     */
    @Override
    public void setGuid(String guid) {
        if (guidProperty != null) {
            guidProperty.set(guid);
        } else {
            String old = this.guid;
            this.guid = guid;
            Changes.valueChanged(this, ModelProperty.GUID, old, guid);
        }
    }

    public Optional<String> getId() {
        return Optional.ofNullable(idProperty != null ? idProperty.get() : id);
    }

    public Optional<String> getGuid() {
        return Optional.ofNullable(guidProperty != null ? guidProperty.get() : guid);
    }

    /**
     * The LIFT id, read-only: it changes through {@link #setId(String)}, which keeps the
     * dictionary's indexes in step.
     */
    public ReadOnlyObjectProperty<String> idProperty() {
        if (idProperty == null) {
            idProperty = new TrackedProperty<>(this, ModelProperty.LIFT_ID, id);
            id = null;
        }
        return idProperty.getReadOnlyProperty();
    }

    /**
     * The guid; {@code null} when there is none.
     */
    public ObjectProperty<String> guidProperty() {
        if (guidProperty == null) {
            guidProperty = new TrackedProperty<>(this, ModelProperty.GUID, guid);
            guid = null;
        }
        return guidProperty;
    }
}
