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

    /** Declared in the {@code <range>} of the {@code .lift} file, overriding an external ranges file. */
    private boolean declaredInline;

    /**
     * Whether its range declares it: false for a bare feature the reader or a builder
     * created for a value a component uses and the range does not declare.
     */
    private final TrackedProperty<Boolean> declared;

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
        this.declared = new TrackedProperty<>(this, ModelProperty.FEATURE_DECLARED, Boolean.TRUE);
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


    /**
     * Whether the {@code .lift} file declared this feature inline, in the {@code <range>}
     * of a set defined in an external ranges file: it overrides (or adds to) the file's
     * range elements, and is written back after the reference. Always {@code false} for
     * the features the external file declares, and for the bare features created for a
     * value the range does not declare.
     */
    public boolean isDeclaredInline() {
        return declaredInline;
    }

    void setDeclaredInline(boolean declaredInline) {
        this.declaredInline = declaredInline;
    }

    /**
     * Whether the range of this feature declares it - in the header, in an external ranges
     * file, or because it was added explicitly ({@link FeatureSet#addFeature(Feature)}, a
     * {@code FeatureBuilder}).
     *
     * A feature that is not declared is a bare one the reader or a builder created for a
     * value a component uses and its range does not declare (a note type, a part of
     * speech, a trait value...). It is read-only and is not written - the reload creates it
     * the same way - until {@link LiftHeader#declareFeature(Feature)}.
     */
    public boolean isDeclared() {
        return declared.get();
    }

    /** Whether the range declares this feature, read-only: see {@link #isDeclared()}. */
    public ReadOnlyObjectProperty<Boolean> declaredProperty() {
        return declared.getReadOnlyProperty();
    }

    void setDeclared(boolean declared) {
        this.declared.set(declared);
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parentFeatureSet;
    }
}
