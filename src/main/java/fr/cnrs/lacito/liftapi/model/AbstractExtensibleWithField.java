package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

/**
 * Superclass of lift object that can contain {@code field}s.
 */
public abstract sealed class AbstractExtensibleWithField
    extends AbstractExtensibleWithoutField
    implements HasField
    permits AbstractNotable, LiftEtymology,
            LiftNote, LiftPronunciation,
            LiftRelation, LiftVariant,
            LiftHeaderRangeElement, LiftHeaderRange {

    protected final MapProperty<String, LiftField> fieldsProperty =
            new SimpleMapProperty<>(this, "fields", FXCollections.observableHashMap());

    @Override
    public void addField(LiftField f) {
        if (fieldsProperty.containsKey(f.name)) throw new DuplicateTypeException("Duplicate key (" + f.name + ") for field");
        fieldsProperty.put(f.name, f);
        f.setParent(this);
    }

    @Override
    public LiftField getField(String type) {
        if (!fieldsProperty.containsKey(type)) throw new IllegalArgumentException("No field with type: " + type + ".");
        return fieldsProperty.get(type);
    }

    public List<LiftField> getFields() {
        return fieldsProperty.values().stream().toList();
    }

    public MapProperty<String, LiftField> fieldsProperty() {
        return fieldsProperty;
    }
}
