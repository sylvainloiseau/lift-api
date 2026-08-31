package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * An element of a {@link FeatureSet} representing a value in a terminology.
 *
 * Feature correspond to the {@code range-element} XML element in the LIFT serialization format.
 */
public final class Feature extends AbstractExtensibleWithField {

    private String id;
    private final FeatureSet parentFeatureSet;
    private Optional<String> guid = Optional.empty();
    private MultiText label = new MultiText(this);
    private MultiText abbrev = new MultiText(this);
    private Optional<Feature> superOrdinateFeature = Optional.empty();

    /**
     * Creates a new {@code Feature} with the given id and parent {@code FeatureSet}.
     *
     * @param id the id of the feature
     * @param parent the parent {@code FeatureSet}
     */
    public Feature(String id, FeatureSet parent) {
        this.id = id;
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
        return id;
    }

    /**
     * Changes the id of this feature.
     *
     * This method should only be called by {@link
     * FeatureSet#changeFeatureId(Feature, String)}, in
     * order to take care of the id -> LiftHeaderRangeElement mapping returned by  {@link
     * FeatureSet#getFeatures()} in that class.
     */
    protected void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the optional superordinate feature of this feature.
     *
     * @return the superordinate feature, or {@code Optional.empty()} if none
     */
    public Optional<Feature> getSuperOrdinateFeature() {
        return superOrdinateFeature;
    }

    /**
     * Sets the superordinate feature of this feature.
     *
     * @param parent the superordinate feature, or {@code null} to clear
     */
    public void setSuperordinateFeature(Feature parent) {
        if (parent == null) {
            this.superOrdinateFeature = Optional.empty();
        } else {
            this.superOrdinateFeature = Optional.of(parent);
        }
    }

    /**
     * Returns the optional GUID of this feature.
     *
     * @return the GUID, or {@code Optional.empty()} if none
     */
    public Optional<String> getGuid() {
        return guid;
    }

    /**
     * Sets the GUID of this feature.
     *
     * @param guid the GUID, or {@code null} to clear
     */
    public void setGuid(String guid) {
        if (guid == null) {
            this.guid = Optional.empty();
        } else {
            this.guid = Optional.of(guid);
        }
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

}
