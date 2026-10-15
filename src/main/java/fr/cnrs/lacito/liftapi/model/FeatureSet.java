package fr.cnrs.lacito.liftapi.model;

import java.util.HashSet;
import java.util.Optional;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.FeatureAdded;
import fr.cnrs.lacito.liftapi.change.FeatureRemoved;
import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;


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

    private final TrackedProperty<String> href;

    private boolean externalContentLoaded = false;

    private final TrackedProperty<String> guid;

    MultiText label = new MultiText(this);

    MultiText abbrev = new MultiText(this);

    /** The features, by id: a map, their order is not data. */
    private final ObservableMap<String, Feature> featureMap = FXCollections.observableHashMap();
    private ObservableMap<String, Feature> featureMapView;

    private ReadOnlySetWrapper<Feature> featureSet = null;

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
        this.href = new TrackedProperty<>(this, ModelProperty.HREF, null);
        this.guid = new TrackedProperty<>(this, ModelProperty.GUID, null);
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
        return Optional.ofNullable(guid.get());
    }

    /**
     * The guid of this {@code FeatureSet}.
     *
     * @param guid the guid or {@code null} to clear
     */
    public void setGuid(String guid) {
        this.guid.set(guid);
    }

    /** The guid; {@code null} when there is none. */
    public ObjectProperty<String> guidProperty() {
        return guid;
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
        this.href.set(href);
    }

    /** The href; {@code null} when there is none. */
    public ObjectProperty<String> hrefProperty() {
        return href;
    }

    /**
     * The href of this {@code FeatureSet}.
     *
     * @return the href as an {@code Optional}
     */
    public Optional<String> getHref() {
        return Optional.ofNullable(href.get());
    }

    /**
     * Whether the content of the external file named by {@link #getHref()} has been
     * read into this {@code FeatureSet}.
     *
     * A range declared as {@code <range id="..." href="..."/>} carries its features
     * in a separate {@code .lift-ranges} file. The reader does not load those files
     * yet, so this is {@code false} for every externally-defined range. Serialization
     * relies on it: rewriting a ranges file whose content was never read would
     * replace the user's ranges with an empty shell.
     *
     * @return {@code true} if the external content is present in this object
     */
    public boolean isExternalContentLoaded() {
        return externalContentLoaded;
    }

    /**
     * Record that the content of the external file named by {@link #getHref()} has
     * been read into this {@code FeatureSet}.
     *
     * @param loaded whether the external content is present in this object
     */
    public void setExternalContentLoaded(boolean loaded) {
        this.externalContentLoaded = loaded;
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
     * The features by id, as a read-only observable map; change them with
     * {@link #addFeature(Feature)}, {@link #removeFeature(String)} and
     * {@link #changeFeatureId(Feature, String)}.
     */
    public ObservableMap<String, Feature> getFeatures() {
        if (featureMapView == null) featureMapView = FXCollections.unmodifiableObservableMap(featureMap);
        return featureMapView;
    }

    /**
     * The features as a read-only set property.
     */
    public ReadOnlySetProperty<Feature> featuresProperty() {
        if (featureSet == null) {
            initFeatureSet();
        }
        return featureSet.getReadOnlyProperty();
    }

    /**
     * Returns the feature with the given id.
     *
     * Use {@link #getOrCreateFeature(String)} to create the feature instead of failing.
     *
     * @param id the feature id
     * @return the feature
     * @throws IllegalArgumentException if no feature with the given id exists
     */
    public Feature getFeature(String id) {
        if (!hasFeature(id)) {
            throw new IllegalArgumentException(
                "No feature with id '" + id + "' in feature set '" + this.id + "'"
            );
        }
        return featureMap.get(id);
    }

    /**
     * Returns the feature with the given id, creating it if no such feature exists.
     *
     * @param id the feature id
     * @return the existing or newly created feature
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
     * Once this set is in the header of a dictionary, the texts of the feature are
     * registered as meta-language texts of that dictionary.
     *
     * @param element the feature to add
     * @throws IllegalArgumentException if a feature with the same id already exists
     */
    public void addFeature(Feature element) {
        if (hasFeature(element.getId()))
            throw new IllegalArgumentException("Duplicate feature in feature set: " + element.getId());
        featureMap.put(element.getId(), element);
        LiftDictionary dictionary = headerDictionary();
        if (dictionary != null) {
            dictionary.getMutator().registerHeaderTexts(element);
        }
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureAdded(observer, this, element));
        }
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
        LiftDictionary dictionary = headerDictionary();
        if (dictionary != null) {
            dictionary.getMutator().releaseHeaderTexts(featureMap.get(id));
        }
        Feature removed = featureMap.remove(id);
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new FeatureRemoved(observer, this, removed));
        }
        return removed;
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
        featureMap.put(newId, element);
        // Last, so that the change is reported once the feature is under its new key.
        element.setId(newId);
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

    /**
     * The dictionary whose header holds this feature set, or {@code null} while the set
     * is not in a header yet (being built): the texts of its features are registered
     * when the set joins the header, not before.
     */
    private LiftDictionary headerDictionary() {
        return parent != null && parent.holds(this) ? parent.getDictionary() : null;
    }

    private void initFeatureSet() {
        // The property wraps a read-only view of a set this listener keeps in step.
        ObservableSet<Feature> backing = FXCollections.observableSet(new HashSet<>(featureMap.values()));
        featureSet = new ReadOnlySetWrapper<>(this, "features", FXCollections.unmodifiableObservableSet(backing));
        featureMap.addListener((MapChangeListener<String, Feature>) change -> {
            if (change.wasRemoved()) {
                backing.remove(change.getValueRemoved());
            }
            if (change.wasAdded()) {
                backing.add(change.getValueAdded());
            }
        });
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
