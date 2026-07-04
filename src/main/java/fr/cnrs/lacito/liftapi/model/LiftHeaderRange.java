package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
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

    private final SimpleMapProperty<
        String,
        LiftHeaderRangeElement
    > rangeElements = new SimpleMapProperty<>(
        FXCollections.emptyObservableMap()
    );

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

    public MapProperty<String, LiftHeaderRangeElement> getRangeElements() {
        return rangeElements;
    }

    public LiftHeaderRangeElement getRangeElement(String id) {
        if (!hasRangeElements(id)) {
            throw new IllegalArgumentException(
                "No range element with id: " + id
            );
        }
        return rangeElements.get(id);
    }

    public boolean hasRangeElements(String id) {
        return rangeElements.containsKey(id);
    }

    public void addRangeElement(LiftHeaderRangeElement element) {
        rangeElements.put(element.getId(), element);
    }

    public LiftHeaderRangeElement createRangeElement(String id) {
        LiftHeaderRangeElement e = new LiftHeaderRangeElement(id, this);
        rangeElements.put(id, e);
        return e;
    }

    public void addRangeElement(String id) {
        rangeElements.put(id, new LiftHeaderRangeElement(id, this));
    }

    public void removeRangeElement(String id) {
        rangeElements.remove(id);
    }

    public Optional<String> getGuid() {
        return this.guid;
    }

    public Optional<String> getHref() {
        return this.href;
    }

    public void changeElementId(LiftHeaderRangeElement element, String newId) {
        rangeElements.remove(element.getId());
        element.setId(newId);
        rangeElements.put(element.getId(), element);
    }
}
