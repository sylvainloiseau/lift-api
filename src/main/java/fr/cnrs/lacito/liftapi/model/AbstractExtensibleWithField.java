package fr.cnrs.lacito.liftapi.model;

import java.util.Map;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

/**
 * Superclass of components that can contain {@code LiftField}s.
 */
public abstract sealed class AbstractExtensibleWithField
    extends AbstractExtensibleWithoutField
    implements HasField
    permits AbstractNotable, LiftEtymology,
            LiftNote, LiftPronunciation,
            LiftRelation, LiftVariant,
            Feature, FeatureSet {

    protected final MapProperty<String, LiftField> fieldsProperty =
            new SimpleMapProperty<>(this, "fields", FXCollections.observableHashMap());

    @Override
    public void addField(LiftField f) {
        // The map is keyed by the definition *name*; looking the definition object up
        // instead never matched (LiftFieldAndTraitDefinition has no equals), so a
        // duplicate field silently overwrote the previous one.
        String name = f.getSpecification().getName();
        if (fieldsProperty.containsKey(name)) throw new DuplicateTypeException(
            "Duplicate key (" + name + ") for field"
        );
        fieldsProperty.put(name, f);
        f.setParent(this);
        adopted(f);
    }

    @Override
    public LiftField getField(String type) {
        if (!fieldsProperty.containsKey(type)) throw new IllegalArgumentException("No field with type: " + type + ".");
        return fieldsProperty.get(type);
    }

    @Override
    public Map<String, LiftField> getFields() {
        return fieldsProperty.get();
    }

    public MapProperty<String, LiftField> fieldsProperty() {
        return fieldsProperty;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a field from this component, unregistering it if this component belongs to
     * a dictionary.
     *
     * @param f a field of this component
     */
    @Override
    public void deleteField(LiftField f) {
        requireChild(
            f,
            f != null && fieldsProperty.get(f.getSpecification().getName()) == f
        );
        orphaned(f);
        f.detach();
    }
}
