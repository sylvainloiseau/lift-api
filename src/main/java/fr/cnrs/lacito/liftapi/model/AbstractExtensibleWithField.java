package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

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

    /** The fields, by the name of their definition: a map, their order is not data. */
    final ObservableMap<String, LiftField> fields = FXCollections.observableHashMap();
    private ObservableMap<String, LiftField> fieldsView;

    @Override
    public void addField(LiftField f) {
        ExternalDefinitionGuard.requireWritable(this);
        // The map is keyed by the definition *name*; looking the definition object up
        // instead never matched (LiftFieldAndTraitDefinition has no equals), so a
        // duplicate field silently overwrote the previous one.
        String name = f.getName();
        if (fields.containsKey(name)) throw new DuplicateTypeException(
            "Duplicate key (" + name + ") for field"
        );
        fields.put(name, f);
        f.setParent(this);
        adopted(f);
    }

    @Override
    public LiftField getField(String type) {
        if (!fields.containsKey(type)) throw new IllegalArgumentException("No field with type: " + type + ".");
        return fields.get(type);
    }

    /**
     * @return the fields by name, as a read-only observable map; change them with
     *         {@link #addField} and {@link #deleteField}
     */
    @Override
    public ObservableMap<String, LiftField> getFields() {
        if (fieldsView == null) {
            fieldsView = FXCollections.unmodifiableObservableMap(fields);
        }
        return fieldsView;
    }

    /**
     * @deprecated use {@link #getFields()}, which is observable; this returns a read-only
     *             property over it, and will be removed
     */
    @Deprecated(forRemoval = true)
    public ReadOnlyMapProperty<String, LiftField> fieldsProperty() {
        return new ReadOnlyMapWrapper<>(this, "fields", getFields()).getReadOnlyProperty();
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
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(
            f,
            f != null && fields.get(f.getName()) == f
        );
        removeChild(f);
    }
}
