package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import lombok.Getter;

public final class LiftHeaderRangeElement extends AbstractExtensibleWithField{
    @Getter final String id;
    final LiftHeaderRange parent;

    Optional<String> parentRange = Optional.empty();
    @Getter Optional<String> guid = Optional.empty();

    @Getter MultiText label = new MultiText();
    @Getter MultiText abbrev = new MultiText();
    
    protected LiftHeaderRangeElement(String id, LiftHeaderRange parent) {
        this.id = id;
        this.parent = parent;
    }
    
    public void setOtherParent(String otherParent) {
        this.parentRange = Optional.of(otherParent);
    }

    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

}
