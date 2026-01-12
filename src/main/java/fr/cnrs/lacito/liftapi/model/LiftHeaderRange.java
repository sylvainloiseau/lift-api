package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LiftHeaderRange extends AbstractExtensibleWithField {
    final String id;
    final LiftHeader parent;

    Optional<String> href = Optional.empty();
    Optional<String> guid = Optional.empty();
    
    MultiText label = new MultiText();
    MultiText abbrev = new MultiText();
    List<LiftHeaderRangeElement> rangeContent = new ArrayList<>();

    protected LiftHeaderRange(String id, LiftHeader parent) {
        this.id = id;
        this.parent = parent;
    }

    public void setHref(String href) {
        this.href = Optional.of(href);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public MultiText getLabel() {
        return label;
    }

    public MultiText getAbbrev() {
        return abbrev;
    }
    public List<LiftHeaderRangeElement> getRangeElements() {
        return rangeContent;        
    }

    public String getId() {
        return this.id;
    }

    public Optional<String> getGuid() {
        return this.guid;
    }

    public Optional<String> getHref() {
        return this.href;
    }

}
