package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

public final class LiftHeaderRange extends AbstractExtensibleWithField {

    @Getter
    final String id;

    final LiftHeader parent;

    Optional<String> href = Optional.empty();
    Optional<String> guid = Optional.empty();

    @Getter
    MultiText label = new MultiText();

    @Getter
    MultiText abbrev = new MultiText();

    List<LiftHeaderRangeElement> rangeContent = new ArrayList<>();

    public LiftHeaderRange(String id, LiftHeader parent) {
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

    public List<LiftHeaderRangeElement> getRangeElements() {
        return rangeContent;
    }

    public Optional<String> getGuid() {
        return this.guid;
    }

    public Optional<String> getHref() {
        return this.href;
    }
}
