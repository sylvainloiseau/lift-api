package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

public final class LiftHeaderRangeElement extends AbstractExtensibleWithField {

    private String id;

    public String getId() {
        return id;
    }

    private final LiftHeaderRange parentRange;

    /** The id of the parent range-element (for hierarchical organisation). */
    private Optional<LiftHeaderRangeElement> parentElement = Optional.empty();

    public Optional<LiftHeaderRangeElement> getParentId() {
        return parentElement;
    }

    Optional<String> guid = Optional.empty();

    public Optional<String> getGuid() {
        return guid;
    }

    MultiText label = new MultiText();

    public MultiText getLabel() {
        return label;
    }

    MultiText abbrev = new MultiText();

    public MultiText getAbbrev() {
        return abbrev;
    }

    public LiftHeaderRangeElement(String id, LiftHeaderRange parent) {
        this.id = id;
        this.parentRange = parent;
    }

    public LiftHeaderRange getParentRange() {
        return parentRange;
    }

    /**
     * A parent range-element (for hierarchical organisation of range-element).
     */
    public void setParentElement(LiftHeaderRangeElement parent) {
        this.parentElement = Optional.of(parent);
    }

    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    /**
     * This method should only be called by {@link
     * LiftHeaderRange#changeElementId(LiftHeaderRangeElement, String)}, in
     * order to take care of the id -> LiftHeaderRangeElement mapping in {@link
     * LiftHeaderRange#rangeContent} in that class.
     */
    protected void setId(String id) {
        this.id = id;
    }
}
