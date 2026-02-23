package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import lombok.Getter;

public final class LiftHeaderRangeElement extends AbstractExtensibleWithField {
    @Getter final String id;
    final LiftHeaderRange parentRange;

    /** The id of the parent range-element (for hierarchical organisation). */
    @Getter Optional<String> parentId = Optional.empty();
    @Getter Optional<String> guid = Optional.empty();

    @Getter MultiText label = new MultiText();
    @Getter MultiText abbrev = new MultiText();

    protected LiftHeaderRangeElement(String id, LiftHeaderRange parent) {
        this.id = id;
        this.parentRange = parent;
    }

    public LiftHeaderRange getParentRange() { return parentRange; }

    /** Set the {@code @parent} attribute (id of parent range-element). */
    public void setParentId(String parentId) {
        this.parentId = Optional.ofNullable(parentId);
    }

    /** @deprecated Use {@link #setParentId(String)} — kept for existing callers. */
    @Deprecated
    public void setOtherParent(String otherParent) {
        setParentId(otherParent);
    }

    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }
}
