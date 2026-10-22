package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;

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
 * <li> The key (the name) indicates which values are available. Its specification
 *   (see {@link LiftTrait#getSpecification()}) is a {@link TraitSpecification}: a
 *   {@link LiftFieldAndTraitDefinition} declared in the header (possibly with a
 *   {@link FeatureSet} as its range), a {@link RangeReference} when the name is the id
 *   of a range (the value is one of its features), or an {@link UndeclaredName} (the
 *   value is a string). See below for more details.</li>
 * </ul>
 * </li>
 * <li>A <strong>{@link LiftField}</strong> is a key associated with an open value (text, date, integer).
 * <ul>
 * <li> The key must be unique on the object that receive the field.</li>
 * <li> The key is a {@link FieldSpecification}
 *   (see {@link LiftField#getSpecification()}): a {@link LiftFieldAndTraitDefinition},
 *   or an {@link UndeclaredName}</li>
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
 * The specification gives the data model of the trait: the one of the definition,
 * {@link LiftFieldAndTraitDefinitionDataModel#FEATURE} for a range, or
 * {@link LiftFieldAndTraitDefinitionDataModel#STRING} for an undeclared name (see
 * {@link TraitSpecification#getDataModel()} and the documentation of
 * {@link LiftFieldAndTraitDefinitionDataModel} for the various data models).
 *
 * According to the datamodel, the string provided to {@link #setValue(String)} is parsed differently. For instance,
 * if the data model is {@link LiftFieldAndTraitDefinitionDataModel#FEATURE_SET}, the value is interpreted
 * as a list of feature ids to be found in the range of {@link TraitSpecification#getResolvedFeatureSet()}.
 * See {@link #parseValue(TraitSpecification, String)} for the format.
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

    final ObservableList<LiftAnnotation> annotations = FXCollections.observableArrayList();
    private ObservableList<LiftAnnotation> annotationsView;

    protected HasTrait parent;

    /**
     * The specification of this trait: what its name refers to. It never changes: the
     * value is held in a property whose kind depends on the data model. When the header
     * changes what the name refers to, the trait is replaced (see {@link LiftHeader}).
     */
    private final TraitSpecification specification;

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
     * The specification should be what {@link LiftHeader#resolveTraitSpecification(String)}
     * returns for the name: a trait built with another one is written under its name, and
     * read back with the specification the header gives that name.
     *
     * @param spec the specification of this trait
     * @throws IllegalArgumentException if the specification has a field's data model
     */
    public LiftTrait(TraitSpecification spec) {
        if (spec == null) throw new IllegalArgumentException("The specification of a trait cannot be null");
        this.specification = spec;
        switch (spec.getDataModel()) {
            case STRING -> this.stringValueProperty = new TrackedStringProperty(this, ModelProperty.TRAIT_VALUE, "");
            case INTEGER -> this.integerProperty = new TrackedIntegerProperty(this, ModelProperty.TRAIT_VALUE, 0);
            case DATETIME -> this.dateTimeProperty = new TrackedProperty<ZonedDateTime>(this, ModelProperty.TRAIT_VALUE, null);
            case FEATURE -> this.featureProperty = new TrackedProperty<Feature>(this, ModelProperty.TRAIT_VALUE, null);
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
            case MULTISTRING, MULTITEXT -> throw new IllegalArgumentException(
                "A trait cannot have a field's data model: " + spec.getName());
        }
        trackCollectionValue();
    }

    public LiftTrait(TraitSpecification def, String value) {
        this(def);
        this.stringValueProperty.set(value);
    }

    public LiftTrait(TraitSpecification def, ZonedDateTime value) {
        this(def);
        this.dateTimeProperty.set(value);
    }

    public LiftTrait(TraitSpecification def, Integer i) {
        this(def);
        this.integerProperty.set(i);
    }

    public LiftTrait(TraitSpecification def, Feature rangeElement) {
        this(def);
        this.featureProperty.set(rangeElement);
    }

    public LiftTrait(TraitSpecification def, HashSet<Feature> rangeElementSet) {
        this(def);
        this.featureSetProperty.addAll(rangeElementSet);
    }

    public LiftTrait(TraitSpecification def, List<Feature> rangeElementList) {
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
     * is the ids of the features separated by a comma and a space: this is the value
     * written to the file. Commas rather than whitespace, because feature ids may
     * contain spaces (FLEx semantic domains are {@code "1.5 Plant"}).
     * {@link #parseValue(TraitSpecification, String)} reads it back.
     *
     * @return the value as written in the {@code value} attribute of a {@code <trait>};
     *         the empty string for an option or a date not set
     */
    public String getValue() {
        return switch (specification.getDataModel()) {
            case DATETIME -> this.dateTimeProperty.get() == null ? "" : this.dateTimeProperty.get().toString();
            case STRING -> this.stringValueProperty.get();
            case FEATURE -> this.featureProperty.get() == null ? "" : this.featureProperty.get().getId();
            case FEATURE_SET -> this.featureSetProperty.get().stream()
                .map(Feature::getId)
                .collect(Collectors.joining(", "));
            case FEATURE_LIST -> this.featureListProperty.get().stream()
                .map(Feature::getId)
                .collect(Collectors.joining(", "));
            case INTEGER -> Integer.toString(this.integerProperty.get());
            case MULTISTRING, MULTITEXT -> throw new IllegalStateException(
                "A trait cannot have a field's data model: " + specification.getName());
        };
    }

    /**
     * The value as {@link #getValue()} writes it, or {@code null} when the trait has no
     * value (an option or a date not set yet).
     */
    String getValueOrNull() {
        return getTypedValue() == null ? null : getValue();
    }

    /**
     * Checks that {@code value} can be parsed for {@code specification}, creating
     * nothing: what the header checks before it rebinds traits, so that a rebinding is
     * all or nothing.
     *
     * @param value a value as written; {@code null} for none, always fine
     * @throws IllegalArgumentException if it cannot be parsed, or if the data model takes
     *         features and the specification has no feature set
     */
    static void checkParsable(TraitSpecification specification, String value) {
        if (value == null) {
            return;
        }
        switch (specification.getDataModel()) {
            case FEATURE, FEATURE_SET, FEATURE_LIST -> {
                if (specification.getResolvedFeatureSet().isEmpty()) {
                    throw new IllegalArgumentException(
                        "The trait name " + specification.getName() + " has no feature set to take the value "
                            + value + " from.");
                }
            }
            default -> {
                try {
                    // No feature is created for these data models.
                    parse(specification, value, true);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                        "The value " + value + " cannot be read as " + specification.getDataModel().toStringValue()
                            + " for " + specification.getName() + ".", e);
                }
            }
        }
    }

    /**
     * Parse the given string in order to set the value.
     *
     * The string is parsed as {@link #parseValue(TraitSpecification, String)}
     * does, except that a feature id the trait's feature set does not contain is
     * refused rather than created: this is the method an editing interface calls, and
     * a typo should not silently add a feature to the header.
     *
     * @param value the value; {@code null} stands for an empty string
     * @throws IllegalArgumentException if the value cannot be parsed for the data model
     *         of this trait, or names a feature that does not exist
     */
    public void setValue(String value) {
        setTypedValue(parse(specification, value, false));
    }

    /**
     * Parse a trait value written as a string - the {@code value} attribute of a
     * {@code <trait>} - into the object {@link #setTypedValue(Object)} takes, according
     * to the data model of {@code definition}:
     *
     * <ul>
     * <li>{@code STRING}: the string itself;</li>
     * <li>{@code INTEGER}: an {@code Integer};</li>
     * <li>{@code DATETIME}: a {@code ZonedDateTime}, in ISO format;</li>
     * <li>{@code FEATURE}: the feature of that id in the definition's feature set;</li>
     * <li>{@code FEATURE_SET} and {@code FEATURE_LIST}: a {@code Set} or a {@code List}
     * of features, whose ids are separated by commas; the spaces around each id are
     * trimmed, those inside it kept (feature ids such as FLEx semantic domains,
     * {@code "1.5 Plant"}, contain spaces).</li>
     * </ul>
     *
     * A feature id the feature set does not contain yet is added to it. A file - or a
     * batch import - is the authority on what its values are, so they are declared
     * rather than refused, as the XML reader always did. {@link #setValue(String)} is
     * the strict counterpart.
     *
     * This is the only parser of trait values: the builders and {@link #setValue(String)}
     * both go through it, so that the two cannot drift apart again (they once split
     * multiple values on different separators).
     *
     * @param definition the specification of the trait: a definition, a range, or an
     *        undeclared name (whose values are strings)
     * @param value the value; {@code null} stands for an empty string
     * @return the typed value
     * @throws IllegalArgumentException if the value cannot be parsed for the data model,
     *         or if the data model needs a feature set and the definition has none
     */
    public static Object parseValue(TraitSpecification definition, String value) {
        return parse(definition, value, true);
    }

    private static Object parse(
        TraitSpecification definition,
        String value,
        boolean createFeatures
    ) {
        if (value == null) value = "";
        return switch (definition.getDataModel()) {
            case DATETIME -> ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            case STRING -> value;
            case INTEGER -> Integer.valueOf(value);
            case FEATURE -> feature(definition, value, createFeatures);
            case FEATURE_SET -> new LinkedHashSet<>(features(definition, value, createFeatures));
            case FEATURE_LIST -> features(definition, value, createFeatures);
            case MULTISTRING, MULTITEXT -> throw new IllegalArgumentException(
                "A trait cannot have a field's data model: " + definition.getName());
        };
    }

    private static List<Feature> features(
        TraitSpecification definition,
        String value,
        boolean createFeatures
    ) {
        List<Feature> features = new ArrayList<>();
        for (String id : value.split(",")) {
            id = id.trim();
            if (!id.isEmpty()) {
                features.add(feature(definition, id, createFeatures));
            }
        }
        return features;
    }

    private static Feature feature(
        TraitSpecification definition,
        String id,
        boolean createFeatures
    ) {
        FeatureSet range = definition.getResolvedFeatureSet().orElseThrow(() ->
            new IllegalArgumentException(
                "The trait name " + definition.getName() + " has no feature set to take the value " + id + " from."
            )
        );
        if (createFeatures) {
            return range.getOrCreateUndeclaredFeature(id);
        }
        if (!range.hasFeature(id)) {
            throw new IllegalArgumentException("Feature not known: " + id);
        }
        return range.getFeature(id);
    }

    /**
     * The value, typed after the definition's data model: a {@code String}, an
     * {@code Integer}, a {@code ZonedDateTime}, a {@link Feature} (or {@code null}), or
     * an unmodifiable {@code Set} or {@code List} of features (a copy).
     *
     * This is the value reported in {@link fr.cnrs.lacito.liftapi.change.ValueChanged}
     * events ({@link ModelProperty#TRAIT_VALUE}).
     */
    public Object getTypedValue() {
        return switch (specification.getDataModel()) {
            case STRING -> stringValueProperty.get();
            case INTEGER -> integerProperty.get();
            case DATETIME -> dateTimeProperty.get();
            case FEATURE -> featureProperty.get();
            case FEATURE_SET -> Set.copyOf(featureSetProperty.get());
            case FEATURE_LIST -> List.copyOf(featureListProperty.get());
            case MULTISTRING, MULTITEXT -> throw new IllegalStateException(
                "A trait cannot have a field's data model: " + specification.getName());
        };
    }

    /**
     * Set the value from an object of the type {@link #getTypedValue()} returns.
     *
     * @param value the new value; {@code null} clears an object value, and stands for an
     *        empty string, zero, or an empty collection otherwise
     */
    @SuppressWarnings("unchecked")
    public void setTypedValue(Object value) {
        ExternalDefinitionGuard.requireWritable(this);
        switch (specification.getDataModel()) {
            case STRING -> stringValueProperty.set(value == null ? "" : (String) value);
            case INTEGER -> integerProperty.set(value == null ? 0 : (Integer) value);
            case DATETIME -> dateTimeProperty.set((ZonedDateTime) value);
            case FEATURE -> featureProperty.set((Feature) value);
            case FEATURE_SET -> replaceFeatureSet(value == null ? List.of() : (Collection<Feature>) value);
            case FEATURE_LIST -> featureListProperty.setAll(value == null ? List.of() : (Collection<Feature>) value);
            case MULTISTRING, MULTITEXT -> throw new IllegalStateException(
                "A trait cannot have a field's data model: " + specification.getName());
        }
    }

    private void replaceFeatureSet(Collection<Feature> features) {
        Set<Feature> wanted = new HashSet<>(features);
        featureSetProperty.retainAll(wanted);
        featureSetProperty.addAll(wanted);
    }

    /**
     * The collection-valued traits are reported from a listener on their content (a
     * set or a list changes without its property being invalidated); the other ones by
     * their tracked property.
     */
    private void trackCollectionValue() {
        if (featureSetProperty != null) {
            Object[] last = {Set.copyOf(featureSetProperty)};
            featureSetProperty.addListener((SetChangeListener<Feature>) c -> {
                Object now = Set.copyOf(featureSetProperty);
                Changes.valueChanged(this, ModelProperty.TRAIT_VALUE, last[0], now);
                last[0] = now;
            });
        }
        if (featureListProperty != null) {
            Object[] last = {List.copyOf(featureListProperty)};
            featureListProperty.addListener((ListChangeListener<Feature>) c -> {
                Object now = List.copyOf(featureListProperty);
                Changes.valueChanged(this, ModelProperty.TRAIT_VALUE, last[0], now);
                last[0] = now;
            });
        }
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.STRING)
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.DATETIME)
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.INTEGER)
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.FEATURE)
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.FEATURE_SET)
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
        if (this.specification.getDataModel() != LiftFieldAndTraitDefinitionDataModel.FEATURE_LIST)
            throw new IllegalArgumentException("This trait has not a 'feature list' type.");
        return featureListProperty;
    }
    // --------------------------------------------------------
    // Specification
    // --------------------------------------------------------

    /**
     * What the name of this trait refers to: a definition of the header, a range, or an
     * undeclared name. See {@link TraitSpecification}.
     */
    public TraitSpecification getSpecification() {
        return specification;
    }

    /** The name of this trait, as written in its {@code name} attribute. */
    public String getName() {
        return specification.getName();
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
        addAnnotation(annotations.size(), a);
    }

    @Override
    public void addAnnotation(int index, LiftAnnotation a) {
        ExternalDefinitionGuard.requireWritable(this);
        annotations.add(index, a);
        a.setParent(this);
        adopted(a);
    }

    public ObservableList<LiftAnnotation> getAnnotations() {
        if (annotationsView == null) annotationsView = FXCollections.unmodifiableObservableList(annotations);
        return annotationsView;
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
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(a, annotations.contains(a));
        removeChild(a);
    }

    @Override
    public void retypeAnnotation(LiftAnnotation a, Feature type) {
        ExternalDefinitionGuard.requireWritable(this);
        requireChild(a, annotations.contains(a));
        a.assignType(type);
    }
}
