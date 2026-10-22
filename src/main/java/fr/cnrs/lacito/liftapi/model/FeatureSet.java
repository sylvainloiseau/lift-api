package fr.cnrs.lacito.liftapi.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.FeatureAdded;
import fr.cnrs.lacito.liftapi.change.FeatureRemoved;
import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
  *
  * Its id can change, through {@link LiftHeader#renameFeatureSet(FeatureSet, String)}:
  * what refers to the set - the definitions taking it as their range, the components
  * typed by its features - holds the object, and follows.
  *
  * <h2>Feature sets defined in an external file</h2>
  *
  * A header {@code <range id="..." href="..."/>} takes its content from a
  * {@code .lift-ranges} file, which the reader reads (once per file) and which is never
  * written: on save, the header only writes the reference again. Such a set
  * {@linkplain #isExternallyDefined() is externally defined}, and is read-only: its id,
  * texts, features, their hierarchy and texts, and the traits, fields and annotations they
  * hold cannot change (an {@code IllegalStateException}), since the change would be lost on
  * save. {@link LiftHeader#makeFeatureSetInternal(FeatureSet)} removes the {@code href}:
  * the set is then writable, and written in full in the header.
  *
  * The {@code <range>} of the {@code .lift} file may also have children: they override the
  * external file (LIFT 0.13, §21). An inline range element replaces the file's element of
  * the same id, or adds one; an inline label, description or abbreviation of the range
  * replaces the file's; inline traits, fields and annotations of the range are added. What
  * was declared inline is {@linkplain #isDeclaredInline(Feature) flagged}, and the writer
  * writes it back after the reference.
  *
  * A component may use a value the external range does not declare (FLEx files do): the
  * reader and the builders then add a bare feature (an id only) to the set, which is not
  * written - the reload adds it the same way.
  */
public final class FeatureSet extends AbstractExtensibleWithField {

    /** Changed only through {@link LiftHeader#renameFeatureSet}, which re-keys the header. */
    private final TrackedProperty<String> id;

    final LiftHeader parent;

    /** The {@code href} of the header's {@code <range>}: set by the reader only. */
    private final TrackedProperty<String> href;

    /** The ranges file the {@code href} was resolved to on load; {@code null} if none was found. */
    private Path externalDefinitionFile;

    /** Whether the reader found neither the ranges file nor the range in it. */
    private boolean externalDefinitionMissing;

    /** Which of label, description and abbreviation the {@code .lift} file declared inline. */
    private boolean labelDeclaredInline;
    private boolean descriptionDeclaredInline;
    private boolean abbrevDeclaredInline;

    /** The traits, fields and annotations of this set the {@code .lift} file declared inline. */
    private final Set<AbstractLiftRoot> componentsDeclaredInline =
        Collections.newSetFromMap(new IdentityHashMap<>());

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
        this.id = new TrackedProperty<>(this, ModelProperty.FEATURE_SET_ID, id);
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
        return id.get();
    }

    /**
     * The id, read-only: it changes through
     * {@link LiftHeader#renameFeatureSet(FeatureSet, String)}, which keeps the header's
     * index in step.
     */
    public ReadOnlyObjectProperty<String> idProperty() {
        return id.getReadOnlyProperty();
    }

    /**
     * Changes the id of this feature set. Called only by
     * {@link LiftHeader#renameFeatureSet(FeatureSet, String)}, once the header holds the
     * set under its new id.
     */
    void setId(String newId) {
        this.id.set(newId);
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
     * Sets the {@code href}. Only the reader sets one, once the content of the external
     * file is read into this set; {@link LiftHeader#makeFeatureSetInternal(FeatureSet)}
     * clears it. A public setter would let an internal set become a bare reference, whose
     * content would vanish on save.
     */
    void setHref(String href) {
        this.href.set(href);
    }

    /**
     * The {@code href}, read-only: {@code null} for a set defined in the header.
     */
    public ReadOnlyObjectProperty<String> hrefProperty() {
        return href.getReadOnlyProperty();
    }

    /**
     * The {@code href} of the header's {@code <range>}: the external ranges file this set
     * is defined in.
     *
     * @return the href as an {@code Optional}
     */
    public Optional<String> getHref() {
        return Optional.ofNullable(href.get());
    }

    /**
     * Whether this set is defined in an external ranges file (it has an {@code href}), and
     * is therefore read-only.
     */
    public boolean isExternallyDefined() {
        return href.get() != null;
    }

    /**
     * The ranges file the {@code href} was resolved to when the dictionary was read: what
     * the writer refers to when the dictionary is saved in another directory. Empty if no
     * file was found (the set is then empty) or the set was never external.
     */
    public Optional<Path> getExternalDefinitionFile() {
        return Optional.ofNullable(externalDefinitionFile);
    }

    void setExternalDefinitionFile(Path file) {
        this.externalDefinitionFile = file;
    }

    /**
     * Whether the reader found neither the ranges file the {@code href} names nor, in it, a
     * range of this id: the set is then empty (FLEx leaves empty ranges out of its file).
     */
    public boolean isExternalDefinitionMissing() {
        return externalDefinitionMissing;
    }

    void setExternalDefinitionMissing(boolean missing) {
        this.externalDefinitionMissing = missing;
    }

    /**
     * Whether the {@code .lift} file declared this feature inline, in the {@code <range>}
     * of this externally defined set (it overrides the external file, and is written back).
     */
    public boolean isDeclaredInline(Feature feature) {
        return feature.isDeclaredInline();
    }

    /**
     * Whether the {@code .lift} file declared this text of the set (its label, description
     * or abbreviation) inline, overriding the external file's.
     */
    public boolean isDeclaredInline(MultiText text) {
        return (text == label && labelDeclaredInline)
            || (text == getDescription() && descriptionDeclaredInline)
            || (text == abbrev && abbrevDeclaredInline);
    }

    /**
     * Whether the {@code .lift} file declared this trait, field or annotation of the set
     * inline, in addition to the external file's.
     */
    public boolean isDeclaredInline(AbstractLiftRoot component) {
        return component instanceof Feature f ? f.isDeclaredInline() : componentsDeclaredInline.contains(component);
    }

    /**
     * Records that the {@code .lift} file declared {@code part} inline: a feature, a text
     * of the set, or a trait, field or annotation of the set. Called by the reader.
     */
    void markDeclaredInline(Object part) {
        switch (part) {
            case Feature f -> f.setDeclaredInline(true);
            case MultiText t when t == label -> labelDeclaredInline = true;
            case MultiText t when t == getDescription() -> descriptionDeclaredInline = true;
            case MultiText t when t == abbrev -> abbrevDeclaredInline = true;
            case AbstractLiftRoot c -> componentsDeclaredInline.add(c);
            default -> throw new IllegalArgumentException("Not a part of a feature set: " + part);
        }
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
                "No feature with id '" + id + "' in feature set '" + getId() + "'"
            );
        }
        return featureMap.get(id);
    }

    /**
     * Returns the feature with the given id, creating it if no such feature exists.
     * Refused (an {@code IllegalStateException}) if it does not exist and the set is
     * {@linkplain #isExternallyDefined() externally defined}.
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
     * Returns the feature with the given id, creating a bare one (an id only) if the set
     * has none - even in an externally defined set. For the reader and the builders: a
     * component uses a value its range does not declare. In an external set that is no
     * change of the definition: the feature is not written, and the reload creates it the
     * same way.
     */
    Feature getOrCreateUndeclaredFeature(String id) {
        if (hasFeature(id)) {
            return featureMap.get(id);
        }
        Feature created = new Feature(id, this);
        // Set before it is added, so that a listener of the addition sees it.
        created.setUndeclaredInTheExternalFile(isExternallyDefined());
        ExternalDefinitionGuard.withoutGuard(() -> addFeature(created));
        return created;
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
        ExternalDefinitionGuard.requireWritable(this);
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
     * Once this set is in the header of a dictionary, a feature in use cannot be removed
     * (see {@link LiftHeader#isUsed(Feature)}): what uses it would be left with a feature
     * that no range declares.
     *
     * @param id the feature id
     * @return the removed feature
     * @throws IllegalArgumentException if no feature with the given id exists
     * @throws IllegalStateException if the feature is in use
     */
    public Feature removeFeature(String id) {
        ExternalDefinitionGuard.requireWritable(this);
        if (!hasFeature(id))
            throw new IllegalArgumentException("Cannot remove non-existing feature: " + id);
        LiftDictionary dictionary = headerDictionary();
        if (dictionary != null && parent.isUsed(featureMap.get(id))) {
            throw new IllegalStateException(
                "The feature " + id + " of the feature set " + getId() + " is in use.");
        }
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
        ExternalDefinitionGuard.requireWritable(this);
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
