package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected final Map<String, LiftField> fields = new HashMap<>();

    @Override
    public void addField(LiftField f) {
        if (fields.containsKey(f.name)) throw new DuplicateTypeException("Duplicate key (" + f.name + ") for field");
        fields.put(f.name, f);
        f.setParent(this);
    }

    @Override
    public LiftField getField(String type) {
        if (!fields.containsKey(type)) throw new IllegalArgumentException("No field with type: " + type + ".");
        return fields.get(type);
    }

    public List<LiftField> getFields() {
        return fields.values().stream().toList();
    }


}
