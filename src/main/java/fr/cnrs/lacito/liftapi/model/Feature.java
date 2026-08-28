package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * An element of a {@link FeatureSet} representing a value in a terminology.
 * 
 * Feature correspond to the {@code range-element} XML element in the LIFT serialization format.
 * 
 */
public final class Feature extends AbstractExtensibleWithField {

    private String id;

    public String getId() {
        return id;
    }

    private final FeatureSet parentFeatureSet;

    /** The id of the parent range-element (for hierarchical organisation). */
    private Optional<Feature> superOrdinateFeature = Optional.empty();

    public Optional<Feature> getSuperOrdinateFeature() {
        return superOrdinateFeature;
    }

    Optional<String> guid = Optional.empty();

    public Optional<String> getGuid() {
        return guid;
    }

    MultiText label = new MultiText(this);

    public MultiText getLabel() {
        return label;
    }

    MultiText abbrev = new MultiText(this);

    public MultiText getAbbrev() {
        return abbrev;
    }

    public Feature(String id, FeatureSet parent) {
        this.id = id;
        this.parentFeatureSet = parent;
    }

    /**
     * The {@link FeatureSet} this {@code Feature} belongs to.
     * @return the {@code FeatureSet}
     */
    public FeatureSet getParentFeatureSet() {
        return parentFeatureSet;
    }

    /**
     * A parent range-element (for hierarchical organisation of range-element).
     */
    public void setSuperordinateFeature(Feature parent) {
        if (parent == null) {
            this.superOrdinateFeature = Optional.empty();
        } else {
            this.superOrdinateFeature = Optional.of(parent);
        }
    }

    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    /**
     * This method should only be called by {@link
     * FeatureSet#changeFeatureId(Feature, String)}, in
     * order to take care of the id -> LiftHeaderRangeElement mapping returned by  {@link
     * FeatureSet#getFeatures()} in that class.
     */
    protected void setId(String id) {
        this.id = id;
    }
}
