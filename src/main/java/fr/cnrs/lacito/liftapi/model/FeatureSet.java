package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;

/**
  * A terminology for the classification of components into a fixed set of values ({@link Feature}), for instance part of speech, usage markers, inflexional classes, etc.
  * 
  * FeatureSet correspond to the {@code range} XML element in the LIFT serialization format.
  * 
  * In a {@code FeatureSet}, the {@code Feature} may be organized hierarchically (see {@link Feature#setSuperordinateFeature(Feature)}).
  */
public final class FeatureSet extends AbstractExtensibleWithField {

    final String id;

    public String getId() {
        return id;
    }

    final LiftHeader parent;

    Optional<String> href = Optional.empty();

    Optional<String> guid = Optional.empty();

    MultiText label = new MultiText(this);

    public MultiText getLabel() {
        return label;
    }

    MultiText abbrev = new MultiText(this);

    public MultiText getAbbrev() {
        return abbrev;
    }

    private final SimpleMapProperty<
        String,
        Feature
    > featureMap = new SimpleMapProperty<>(
        FXCollections.observableHashMap()
        //FXCollections.emptyObservableMap()
    );

    private SimpleSetProperty<Feature> featureSet = null;

    public FeatureSet(String id, LiftHeader parent) {
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

    /**
     * The features as a map (feature id -> {@code Feature})
     */
    public MapProperty<String, Feature> getFeatures() {
        return featureMap;
    }

    /**
     * The features as a set (wrapped in a property)
     */
    public SimpleSetProperty<Feature> featuresProperty() {
        if (featureSet == null) {
            initFeatureSet();
        }
        return featureSet;
    }

    public Feature getFeature(String id) {
        if (!hasFeature(id)) {
            throw new IllegalArgumentException(
                "No range element with id: " + id
            );
        }
        return featureMap.get(id);
    }

    public Feature getOrCreateFeature(String id) {
        if (!hasFeature(id)) {
            return addFeature(id);
        }
        return featureMap.get(id);
    }

    public boolean hasFeature(String id) {
        return featureMap.containsKey(id);
    }

    public void addFeature(Feature element) {
        featureMap.put(element.getId(), element);
    }

    public Feature addFeature(String id) {
        Feature e = new Feature(id, this);
        featureMap.put(id, e);
        return e;
    }

    public Feature removeFeature(String id) {
        return featureMap.remove(id);
    }

    public Optional<String> getGuid() {
        return this.guid;
    }

    public Optional<String> getHref() {
        return this.href;
    }

    public void changeFeatureId(Feature element, String newId) {
        if (featureMap.containsKey(newId)) throw new IllegalArgumentException("The id " + newId + " is already used.");
        featureMap.remove(element.getId());
        element.setId(newId);
        featureMap.put(element.getId(), element);
    }

    public void changeFeatureId(String oldId, String newId) {
        if (featureMap.containsKey(newId)) throw new IllegalArgumentException("The id " + newId + " is already used.");
        Feature element = featureMap.get(oldId);
        changeFeatureId(element, newId);
    }

    private void initFeatureSet() {
        featureSet = new SimpleSetProperty<>(
            FXCollections.emptyObservableSet()
        );
        featureSet.addAll(featureMap.values());
        featureMap.addListener(
            new MapChangeListener<String, Feature>() {
                @Override
                public void onChanged(
                    Change<
                        ? extends String,
                        ? extends Feature
                    > change
                ) {
                    if (change.wasAdded()) {
                        featureSet.add(change.getValueAdded());
                    } else if (change.wasRemoved()) {
                        featureSet.remove(change.getValueRemoved());
                    }
                }
            }
        );
    }
}
