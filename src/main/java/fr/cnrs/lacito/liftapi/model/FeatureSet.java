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

    final LiftHeader parent;

    Optional<String> href = Optional.empty();

    Optional<String> guid = Optional.empty();

    MultiText label = new MultiText(this);

    MultiText abbrev = new MultiText(this);

    private final SimpleMapProperty<
        String,
        Feature
    > featureMap = new SimpleMapProperty<>(
        FXCollections.observableHashMap()
        //FXCollections.emptyObservableMap()
    );

    private SimpleSetProperty<Feature> featureSet = null;

    /**
     * Creates a new {@code FeatureSet} with the given id and parent.
     *
     * @param id the id
     * @param parent the parent
     */
    public FeatureSet(
        String id,
        LiftHeader parent
    ) {
        this.id = id;
        this.parent = parent;
    }

    /**
     * The id of this {@code FeatureSet}.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * The guid of this {@code FeatureSet}.
     *
     * @return the guid as an {@code Optional}
     */
    public Optional<String> getGuid() {
        return guid;
    }

    /**
     * The guid of this {@code FeatureSet}.
     *
     * @param guid the guid or {@code null} to clear
     */
    public void setGuid(String guid) {
        if (guid == null) {
            this.guid = Optional.empty();
        } else {
            this.guid = Optional.of(guid);
        }
    }

    /**
     * The label of this {@code FeatureSet}.
     *
     * @return the label as a {@code MultiText}
     */
    public MultiText getLabel() {
        return label;
    }

    /**
     * The abbreviation of this {@code FeatureSet}.
     *
     * @return the abbreviation as a {@code MultiText}
     */
    public MultiText getAbbrev() {
        return abbrev;
    }

    /**
     * The href of this {@code FeatureSet}.
     *
     * @param href the href or {@code null} to clear
     */
    public void setHref(String href) {
        if (href == null) {
            this.href = Optional.empty();
        } else {
            this.href = Optional.of(href);
        }
    }

    /**
     * The href of this {@code FeatureSet}.
     *
     * @return the href as an {@code Optional}
     */
    public Optional<String> getHref() {
        return this.href;
    }

    /**
     * The description of this {@code FeatureSet}.
     *
     * @return the description as a {@code MultiText}
     */
    public MultiText getDescription() {
        return getMainMultiText();
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

    /**
     * Returns the feature with the given id, or throws an exception if no such feature exists.
     *
     * @param id the feature id
     * @return the feature
     * @throws IllegalArgumentException if no feature with the given id exists
     */
    public Feature getFeature(String id) {
        if (!hasFeature(id)) {
            throw new IllegalArgumentException(
                "No range element with id: " + id
            );
        }
        return featureMap.get(id);
    }

    /**
     * Returns the feature with the given id, or creates a new one if no such feature exists.
     *
     * @param id the feature id
     * @return the feature
     */
    public Feature getOrCreateFeature(String id) {
        if (!hasFeature(id)) {
            return addFeature(id);
        }
        return featureMap.get(id);
    }

    /**
     * Returns whether this {@code FeatureSet} has a feature with the given id.
     *
     * @param id the feature id
     * @return {@code true} if the feature exists, {@code false} otherwise
     */
    public boolean hasFeature(String id) {
        return featureMap.containsKey(id);
    }

    /**
     * Adds the given feature to this {@code FeatureSet}.
     *
     * @param element the feature to add
     * @throws IllegalArgumentException if a feature with the same id already exists
     */
    public void addFeature(Feature element) {
        if (hasFeature(element.getId()))
            throw new IllegalArgumentException("Duplicate feature in feature set: " + element.getId());
        featureMap.put(element.getId(), element);
    }

    /**
     * Create and add a feature with the given id to this {@code FeatureSet}.
     *
     * @param id the feature id
     * @return the newly created feature
     * @throws IllegalArgumentException if a feature with the same id already exists
     */
    public Feature addFeature(String id) {
        if (hasFeature(id))
            throw new IllegalArgumentException("Duplicate feature in feature set: " + id);
        Feature e = new Feature(id, this);
        addFeature(e);
        return e;
    }

    /**
     * Removes the feature with the given id from this {@code FeatureSet}.
     *
     * @param id the feature id
     * @return the removed feature
     * @throws IllegalArgumentException if no feature with the given id exists
     */
    public Feature removeFeature(String id) {
        if (!hasFeature(id))
            throw new IllegalArgumentException("Cannot remove non-existing feature: " + id);
        return featureMap.remove(id);
    }

    /**
     * Changes the id of the given feature to the given new id.
     *
     * @param element the feature to change
     * @param newId the new id
     * @throws IllegalArgumentException if the new id is already used, or if no feature with the old id exists
     */
    public void changeFeatureId(Feature element, String newId) {
        if (featureMap.containsKey(newId)) throw new IllegalArgumentException("The id " + newId + " is already used.");
        if (!featureMap.containsKey(element.getId())) throw new IllegalArgumentException("No feature with id " + element.getId() + " found.");
        featureMap.remove(element.getId());
        element.setId(newId);
        featureMap.put(element.getId(), element);
    }

    /**
     * Changes the id of the feature with the given old id to the given new id.
     *
     * @param oldId the old id
     * @param newId the new id
     * @throws IllegalArgumentException if the new id is already used, or if no feature with the old id exists
     */
    public void changeFeatureId(String oldId, String newId) {
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
