package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * An element of a {@link FeatureSet} representing a value in a terminology.
 *
 * Feature correspond to the {@code range-element} XML element in the LIFT serialization format.
 */
public final class Feature extends AbstractExtensibleWithField {

    private final TrackedProperty<String> id;
    private final FeatureSet parentFeatureSet;
    private final TrackedProperty<String> guid;
    private MultiText label = new MultiText(this);
    private MultiText abbrev = new MultiText(this);
    private final TrackedProperty<Feature> superOrdinateFeature;

    /**
     * Creates a new {@code Feature} with the given id and parent {@code FeatureSet}.
     *
     * @param id the id of the feature
     * @param parent the parent {@code FeatureSet}
     */
    public Feature(String id, FeatureSet parent) {
        this.id = new TrackedProperty<>(this, ModelProperty.FEATURE_ID, id);
        this.guid = new TrackedProperty<>(this, ModelProperty.GUID, null);
        this.superOrdinateFeature = new TrackedProperty<>(this, ModelProperty.SUPERORDINATE_FEATURE, null);
        this.parentFeatureSet = parent;
    }

    public FeatureSet getParent() {
        return parentFeatureSet;
    }

    /**
     * Returns the id of this feature.
     *
     * @return the id of the feature
     */
    public String getId() {
        return id.get();
    }

    /**
     * Changes the id of this feature.
     *
     * This method should only be called by {@link
     * FeatureSet#changeFeatureId(Feature, String)}, in
     * order to take care of the id -> LiftHeaderRangeElement mapping returned by  {@link
     * FeatureSet#getFeatures()} in that class.
     */
    void setId(String id) {
        this.id.set(id);
    }

    /**
     * The id, read-only: it changes through {@link FeatureSet#changeFeatureId}, which
     * keeps the feature set's index in step.
     */
    public ReadOnlyObjectProperty<String> idProperty() {
        return id.getReadOnlyProperty();
    }

    /**
     * Returns the optional superordinate feature of this feature.
     *
     * @return the superordinate feature, or {@code Optional.empty()} if none
     */
    public Optional<Feature> getSuperOrdinateFeature() {
        return Optional.ofNullable(superOrdinateFeature.get());
    }

    /**
     * Sets the superordinate feature of this feature.
     *
     * @param parent the superordinate feature, or {@code null} to clear
     */
    public void setSuperordinateFeature(Feature parent) {
        superOrdinateFeature.set(parent);
    }

    /** The parent of this feature in its set's hierarchy; {@code null} for none. */
    public ObjectProperty<Feature> superordinateFeatureProperty() {
        return superOrdinateFeature;
    }

    /**
     * Returns the optional GUID of this feature.
     *
     * @return the GUID, or {@code Optional.empty()} if none
     */
    public Optional<String> getGuid() {
        return Optional.ofNullable(guid.get());
    }

    /**
     * Sets the GUID of this feature.
     *
     * @param guid the GUID, or {@code null} to clear
     */
    public void setGuid(String guid) {
        this.guid.set(guid);
    }

    /** The guid; {@code null} when there is none. */
    public ObjectProperty<String> guidProperty() {
        return guid;
    }

    /**
     * Returns the label of this feature.
     *
     * @return the label
     */
    public MultiText getLabel() {
        return label;
    }

    /**
     * Returns the abbreviation of this feature.
     *
     * @return the abbreviation
     */
    public MultiText getAbbrev() {
        return abbrev;
    }

    /**
     * The {@link FeatureSet} this {@code Feature} belongs to.
     * @return the {@code FeatureSet}
     */
    public FeatureSet getParentFeatureSet() {
        return parentFeatureSet;
    }

    /**
     * Returns the description of this feature.
     *
     * @return the description
     */
    public MultiText getDescription() {
        return getMainMultiText();
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return parentFeatureSet;
    }
}
