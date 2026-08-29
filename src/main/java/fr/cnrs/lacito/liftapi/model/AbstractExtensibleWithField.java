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
        if (fieldsProperty.containsKey(f.specificationProperty().get())) throw new DuplicateTypeException("Duplicate key (" + f.specificationProperty().get() + ") for field");
        fieldsProperty.put(f.specificationProperty().get().getName(), f);
        f.setParent(this);
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
}
