package fr.cnrs.lacito.liftapi.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

/**
 * A Lift trait (not to be confused with {@link LiftNote}, {@link LiftAnnotation}, {@link LiftField}).
 * 
 * The existence of this variety of components (Note, Annotation, Field, Trait), as well as the fact that some of them
 * can annotate others, is one of the complex aspects of the Lift data model.
 * 
 * <ul>
 * <li>A <strong>{@link LiftTrait}</strong> is a key-value pair.
 * <ul>
 * <li> The key doesn't have to be unique on the object that receive the traits: several traits can have the same key on the same object.</li>
 * <li> The key indicate wich set of possible values are avaible: the key is a {@link LiftFieldAndTraitDefinition}
 *   (see {@link LiftTrait#getSpecification()}), which contain a reference to a {@link FeatureSet} taxinomy
 *   (see {@link LiftFieldAndTraitDefinition#getResolvedFeatureSet()}). See below for more details.</li>
 * </ul>
 * </li>
 * <li>A <strong>{@link LiftField}</strong> is a key associated with an open value (text, date, integer).
 * <ul>
 * <li> The key must be unique on the object that receive the field.</li>
 * <li> The key is a {@link LiftFieldAndTraitDefinition}
 *   (see {@link LiftField#getSpecification()}), which indicate the data model (text, date, integer)</li>
 * <li>a (string) value is a multitext.</li>
 * </ul>
 * </li>
 * <li>A <strong>{@link LiftAnnotation}</strong> is a meta-comment about the making of the dictionary.
 * <ul>
 * <li> It contains a type (a {@link Feature}), as well as a date and an annotator name, and a free text</li>
 * <li> The possible type can be managed with the manager ({@link LiftHeader#getAnnotationTypeManager()}).</li>
 * </ul>
 * </li>
 * <li>A <strong>{@link LiftNote}</strong> is a supplementary descriptive material.
 * <ul>
 * <li> It contains a type (a {@link Feature}), as well as a MultiText</li>
 * <li> The possible type can be managed with the manager ({@link LiftHeader#getNoteTypeManager()}).</li>
 * </ul>
 * </li>
 * </ul>
 *
 *
 * The LiffFieldAndTraitDefinition specifies in particular the datamodel of the traits.
 * The different possible datamodel are the values of
 * {@link LiftFieldAndTraitDefinitionDataModel} (see {@link LiftFieldAndTraitDefinition#getDataModel()}). See the
 * documentation of {@link LiftFieldAndTraitDefinitionDataModel} for the various datamodel.
 * 
 * According to the datamodel, the string provided to {@link #setValue(String)} is parsed differently. For instance,
 * if the data model is {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_SET}, the value is interpreted
 * as a whitespace-separated list of RangeElement id to be fount in the range of {@link LiftFieldAndTraitDefinition#getResolvedFeatureSet()}.
 *
 * A Trait can receive annotation, but no notes or fields.
 *
 * <i>A trait is simply a reference to a single range-element in a range. It can be used to give the
 * dialect for a variant or the status of an entry. The semantics of a trait in a particular context
 * are given by the parent object and also by the range and range-element being referred to.
 * Where no range is linked the name is informal or resolved by its use in a field-definition.
 * (Lift specification, p. 13)
 * </i>
 */
public final class LiftTrait extends AbstractLiftRoot implements HasAnnotation {

    protected final List<LiftAnnotation> annotations = new ArrayList<>();

    protected HasTrait parent;

    /**
     * The specification of this trait.
     */
    private final SimpleObjectProperty<LiftFieldAndTraitDefinition> specification;

    private SimpleObjectProperty<ZonedDateTime> dateTimeProperty;
    private StringProperty stringValueProperty;
    private SimpleObjectProperty<Feature> featureProperty;
    private SimpleSetProperty<Feature> featureSetProperty;
    private SimpleListProperty<Feature> featureListProperty;
    private SimpleIntegerProperty integerProperty;

    /**
     * Construct a trait with a trait specification (the specification cannot be changed).
     *
     * The value is left at its empty default for the specification's data model; use
     * one of the value-taking constructors, or {@link #setValue(String)}, to set it.
     *
     * @param spec the specification of this trait
     * @throws IllegalArgumentException if the specification has no legal data model
     */
    public LiftTrait(LiftFieldAndTraitDefinition spec) {
        this.specification = new SimpleObjectProperty<>(this, "definition", spec);
        switch (spec.getDataModel().get()) {
            case STRING -> this.stringValueProperty = new SimpleStringProperty(this, "value", "");
            case INTEGER -> this.integerProperty = new SimpleIntegerProperty(this, "value", 0);
            case DATETIME -> this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", null);
            case FEATURE -> this.featureProperty = new SimpleObjectProperty<>(this, "value", null);
            // A FEATURE_SET is backed by a set and a FEATURE_LIST by a list; the two used
            // to be crossed over, and both wrapped a null collection so any addAll() threw.
            case FEATURE_SET -> this.featureSetProperty = new SimpleSetProperty<>(
                this,
                "value",
                FXCollections.observableSet()
            );
            case FEATURE_LIST -> this.featureListProperty = new SimpleListProperty<>(
                this,
                "value",
                FXCollections.observableArrayList()
            );
            default -> throw new IllegalArgumentException("Unknown definition type: " + spec.getDataModel().get());
        }
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, String value) {
        this(def);
        this.stringValueProperty.set(value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, ZonedDateTime value) {
        this(def);
        this.dateTimeProperty.set(value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Integer i) {
        this(def);
        this.integerProperty.set(i);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Feature rangeElement) {
        this(def);
        this.featureProperty.set(rangeElement);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, HashSet<Feature> rangeElementSet) {
        this(def);
        this.featureSetProperty.addAll(rangeElementSet);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, List<Feature> rangeElementList) {
        this(def);
        this.featureListProperty.addAll(FXCollections.observableList(rangeElementList));
    }

    // --------------------------------------------------------
    // Text
    // --------------------------------------------------------

    /**
     * LiftTrait have no MultiText: call to this method will throw an exception.
     *
     * @throws IllegalStateException always
     */
    @Override
    public MultiText getMainMultiText() {
        throw new IllegalStateException("Trait does not have a main MultiText");
    }

    /**
     * LiftTrait have no MultiText: call to this method will throw an exception.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    protected void addToMainMultiText(Form t) {
        throw new UnsupportedOperationException(
            "Trait does not have a main MultiText"
        );
    }

    // --------------------------------------------------------
    // get and set values as string
    // --------------------------------------------------------

    // private SimpleObjectProperty<ZonedDateTime> dateTimeProperty;
    // private StringProperty stringValueProperty;
    // private SimpleObjectProperty<LiftHeaderRangeElement> rangeElementProperty;

    // private SimpleSetProperty<LiftHeaderRangeElement> rangeElementSetProperty;
    // private SimpleListProperty<LiftHeaderRangeElement> rangeElementListProperty;
    // private SimpleIntegerProperty integerProperty;

    /**
     * Return a textual representation of the value.
     * 
     * For the actual objects,
     * consider using the accessor ({@link #dateTimeValueProperty()},
     * {@link #stringValueProperty()}, etc.) corresponding to the data model of this trait.
     * 
     * For a {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_SET}
     * or a {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_LIST}, it
     * is a comma-separated list of the ID of the features.
     * @return
     */
    public String getValue() {
        return switch (specification.get().getType().get()) {
            case LiftFieldAndTraitDefinitionDataModel.DATETIME -> this.dateTimeProperty.get().toString();
            case LiftFieldAndTraitDefinitionDataModel.STRING -> this.stringValueProperty.get();
            case LiftFieldAndTraitDefinitionDataModel.FEATURE -> this.featureProperty.get().getId();
            case LiftFieldAndTraitDefinitionDataModel.FEATURE_SET -> {
                //throw new UnsupportedOperationException("OPTION_COLLECTION type is not supported for trait value");
                 yield this.featureSetProperty.get().stream()
                     .map(x -> x.getId())
                     .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionDataModel.FEATURE_LIST -> {
                // throw new UnsupportedOperationException("OPTION_SEQUENCE type is not supported for trait value");
                yield this.featureListProperty.get().stream()
                     .map(x -> x.getId())
                  .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionDataModel.INTEGER -> Integer.toString(this.integerProperty.get());
            default -> throw new IllegalArgumentException("Illegal trait type: " + specification.get().getTypeStr());
        };
    }

    /**
     * Parse the given string in order to set the value.
     * 
     * The string will be parsed according to the data model of this trait:
     * 
     * <ul>
     * <li> for a DATETIME, it will be parsed as a date time expression.</li>
     * <li> for a FEATURE, it will be considered as the id of a feature in the feature set of this trait.</li>
     * <li> for a FEATURE_SET, it will be considered as the comma separated set of feature ids belonging to the feature set of this trait.</li>
     * <li>etc.</li>
     * </ul>
     * 
     * @param value
     */
    // TODO complete the implementation
    public void setValue(String value) {
        if (value == null) value = "";
        //valueProperty.set(value);
        switch (specification.get().getType().get()) {
            case LiftFieldAndTraitDefinitionDataModel.DATETIME -> this.dateTimeProperty.set(ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME));
            case LiftFieldAndTraitDefinitionDataModel.STRING -> this.stringValueProperty.set(value);
            case LiftFieldAndTraitDefinitionDataModel.FEATURE -> {
                if (specification.get().getResolvedFeatureSet().get().hasFeature(value)) {
                    Feature f = specification.get().getResolvedFeatureSet().get().getFeature(value);
                    featureProperty.set(f);
                } else {
                    throw new IllegalArgumentException("Feature not known: + " + value);
                }
            }
            case LiftFieldAndTraitDefinitionDataModel.FEATURE_SET -> {
                featureSetProperty.addAll(getFeaturesFromString(value));
            }
            case LiftFieldAndTraitDefinitionDataModel.FEATURE_LIST -> {
                featureListProperty.addAll(getFeaturesFromString(value));
            }
            case LiftFieldAndTraitDefinitionDataModel.INTEGER -> this.integerProperty.set(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Illegal trait type: " + specification.get().getTypeStr());
        }
    }

    private List<Feature> getFeaturesFromString(String value) {
        String[] fIds = value.split(",");
        Feature[] features = new Feature[fIds.length];
        int i = 0;
        for (String fId : fIds) {
            fId = fId.trim();
            if (fId.isEmpty()) continue;
            if (specification.get().getResolvedFeatureSet().get().hasFeature(fId)) {
                Feature f = specification.get().getResolvedFeatureSet().get().getFeature(fId);
                features[i] = f;
                i++;
            } else {
                throw new IllegalArgumentException("Feature not known: + " + fId);
            }
        }
        return Arrays.asList(Arrays.<Feature>copyOf(features, i));
    }

    // --------------------------------------------------------
    // Accessors for the actual value property
    // --------------------------------------------------------

    /**
     * Return the string value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#STRING}.
     * 
     * @return a StringProperty
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#STRING}
     */
    public StringProperty stringValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.STRING)
            throw new IllegalArgumentException("This trait has not a 'string' type.");
        return stringValueProperty;
    }

    /**
     * Return the date time value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#DATETIME}.
     * 
     * @return a datetime
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#DATETIME}
     */
    public SimpleObjectProperty<ZonedDateTime> dateTimeValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.DATETIME)
            throw new IllegalArgumentException("This trait has not a 'datetime' type.");
        return dateTimeProperty;
    }

    /**
     * Return the integer value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#INTEGER}.
     * 
     * @return an integer property
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#INTEGER}
     */
    public SimpleIntegerProperty integerValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.INTEGER)
            throw new IllegalArgumentException("This trait has not a 'integer' type.");
        return integerProperty;
    }

    /**
     * Return the feature value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE}.
     * 
     * @return an feature property
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE}
     */
    public SimpleObjectProperty<Feature> featureValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.FEATURE)
            throw new IllegalArgumentException("This trait has not a 'feature' type.");
        return featureProperty;
    }

    /**
     * Return the feature set value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_SET}.
     * 
     * @return an feature set property
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_SET}
     */
    public SimpleSetProperty<Feature> featureSetValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.FEATURE_SET)
            throw new IllegalArgumentException("This trait has not a 'feature set' type.");
        return featureSetProperty;
    }

    /**
     * Return the feature list value property of this trait is the data model of the
     * trait is of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_LIST}.
     * 
     * @return an feature list property
     * @throws IllegalArgumentException if the data model of this trait is
     * not of type {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_LIST}
     */
    public SimpleListProperty<Feature> featureListValueProperty() {
        if (this.specification.get().getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.FEATURE_LIST)
            throw new IllegalArgumentException("This trait has not a 'feature list' type.");
        return featureListProperty;
    }
    // --------------------------------------------------------
    // Specification
    // --------------------------------------------------------

    public LiftFieldAndTraitDefinition getSpecification() {
        return specification.get();
    }

    // --------------------------------------------------------
    // Parent
    // --------------------------------------------------------

    /**
     * Protected: this method is called by the parent when it adopts this LiftTrait.
     * 
     * @param parent the new parent, or {@code null} when detaching this trait
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(HasTrait parent) {
        this.parent = parent;
    }

    public HasTrait getParent() {
        return parent;
    }

    // --------------------------------------------------------
    // Annotations
    // --------------------------------------------------------

    @Override
    public void addAnnotation(LiftAnnotation a) {
        this.annotations.add(a);
        a.setParent(this);
        adopted(a);
    }

    public List<LiftAnnotation> getAnnotations() {
        return annotations;
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove an annotation from this trait, unregistering it if this trait belongs to a
     * dictionary.
     *
     * @param a an annotation of this trait
     */
    @Override
    public void deleteAnnotation(LiftAnnotation a) {
        requireChild(a, annotations.contains(a));
        orphaned(a);
        a.detach();
    }
}
