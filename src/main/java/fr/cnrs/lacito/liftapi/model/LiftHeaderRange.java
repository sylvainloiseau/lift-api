package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;

public final class LiftHeaderRange extends AbstractExtensibleWithField {

    final String id;

    public String getId() {
        return id;
    }

    final LiftHeader parent;

    Optional<String> href = Optional.empty();

    Optional<String> guid = Optional.empty();

    MultiText label = new MultiText();

    public MultiText getLabel() {
        return label;
    }

    MultiText abbrev = new MultiText();

    public MultiText getAbbrev() {
        return abbrev;
    }

    private final SimpleMapProperty<
        String,
        LiftHeaderRangeElement
    > rangeElements = new SimpleMapProperty<>(
        FXCollections.observableHashMap()
        //FXCollections.emptyObservableMap()
    );

    private SimpleSetProperty<LiftHeaderRangeElement> types = null;

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

    public SimpleSetProperty<LiftHeaderRangeElement> typesProperty() {
        if (types == null) {
            initTypes();
        }
        return types;
    }

    public LiftHeaderRangeElement getRangeElement(String id) {
        if (!hasRangeElements(id)) {
            throw new IllegalArgumentException(
                "No range element with id: " + id
            );
        }
        return rangeElements.get(id);
    }

    public LiftHeaderRangeElement getOrCreateRangeElement(String id) {
        if (!hasRangeElements(id)) {
            return createRangeElement(id);
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
        if (rangeElements.containsKey(newId)) throw new IllegalArgumentException("The id " + newId + " is already used.");
        rangeElements.remove(element.getId());
        element.setId(newId);
        rangeElements.put(element.getId(), element);
    }

    public void changeElementId(String oldId, String newId) {
        if (rangeElements.containsKey(newId)) throw new IllegalArgumentException("The id " + newId + " is already used.");
        LiftHeaderRangeElement element = rangeElements.get(oldId);
        changeElementId(element, newId);
    }

    private void initTypes() {
        types = new SimpleSetProperty<>(
            FXCollections.emptyObservableSet()
        );
        types.addAll(rangeElements.values());
        rangeElements.addListener(
            new MapChangeListener<String, LiftHeaderRangeElement>() {
                @Override
                public void onChanged(
                    Change<
                        ? extends String,
                        ? extends LiftHeaderRangeElement
                    > change
                ) {
                    if (change.wasAdded()) {
                        types.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        types.remove(change.getValueRemoved());
                    }
                }
            }
        );
    }

}
