package fr.cnrs.lacito.liftapi.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
 *   (see {@link LiftTrait#getDefinition()}), which contain a reference to a {@link FeatureSet} taxinomy
 *   (see {@link LiftFieldAndTraitDefinition#getResolvedRange()}). See below for more details.</li>
 * </ul>
 * </li>
 * <li>A <strong>{@link LiftField}</strong> is a key associated with an open value (text, date, integer).
 * <ul>
 * <li> The key has to be unique on the object that receive the field.</li>
 * <li> The key is a {@link LiftFieldAndTraitDefinition}
 *   (see {@link LiftField#getType()}), which indicate the data model (text, date, integer)</li>
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
 * if the data model is {@link LiftFieldAndTraitDefinitionDataModel#OPTION_COLLECTION}, the value is interpreted
 * as a whitespace-separated list of RangeElement id to be fount in the range of {@link LiftFieldAndTraitDefinition#getResolvedRange()}.
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
     * The definition of this trait.
     */
    private SimpleObjectProperty<LiftFieldAndTraitDefinition> definitionProperty;

    private SimpleObjectProperty<ZonedDateTime> dateTimeProperty;
    private StringProperty stringValueProperty;
    private SimpleObjectProperty<Feature> rangeElementProperty;
    private SimpleSetProperty<Feature> rangeElementSetProperty;
    private SimpleListProperty<Feature> rangeElementListProperty;
    private SimpleIntegerProperty integerProperty;

    public LiftTrait(LiftFieldAndTraitDefinition def) {
        this.definitionProperty = new SimpleObjectProperty<>(this, "definition", def);
        switch (def.getDataModel().get()) {
            case STRING -> this.stringValueProperty = new SimpleStringProperty(this, "value", "");
            case INTEGER -> this.integerProperty = new SimpleIntegerProperty(this, "value", 0);
            case DATETIME -> this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", null);
            case OPTION -> this.rangeElementProperty = new SimpleObjectProperty<>(this, "value", null);
            case OPTION_COLLECTION -> this.rangeElementListProperty = new SimpleListProperty<>(this, "value", null);
            case OPTION_SEQUENCE -> this.rangeElementSetProperty = new SimpleSetProperty<>(this, "value", null);
            default -> throw new IllegalArgumentException("Unknown definition type: " + def.getDataModel().get());
        }
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, String value) {
        this(def);
        if (!def.getDataModel().isEmpty() && def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.STRING) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with a String value ");
        }
        this.stringValueProperty = new SimpleStringProperty(this, "value", value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, ZonedDateTime value) {
        this(def);
        if (def.getDataModel().isEmpty() || def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.DATETIME) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with a date times value ");
        }
        this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Integer i) {
        this(def);
        if (def.getDataModel().isEmpty() || def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.INTEGER) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with an integer value ");
        }
        this.integerProperty = new SimpleIntegerProperty(this, "value", i);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Feature rangeElement) {
        this(def);
        if (def.getDataModel().isEmpty() || def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.OPTION) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with a range value ");
        }
        this.rangeElementProperty = new SimpleObjectProperty<>(this, "value", rangeElement);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, HashSet<Feature> rangeElementSet) {
        this(def);
        if (def.getDataModel().isEmpty() || def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.OPTION_COLLECTION) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with a set of range value ");
        }
        this.rangeElementSetProperty = new SimpleSetProperty<Feature>(this, "value", FXCollections.observableSet(rangeElementSet));
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, List<Feature> rangeElementList) {
        this(def);
        if (def.getDataModel().isEmpty() || def.getDataModel().get() != LiftFieldAndTraitDefinitionDataModel.OPTION_SEQUENCE) {
            throw new IllegalArgumentException("The Datamodel of this LiftTrait is not compatible with a list of range value ");
        }
        this.rangeElementListProperty = new SimpleListProperty<Feature>(this, "value", FXCollections.observableList(rangeElementList));
    }

    @Override
    public MultiText getMainMultiText() {
        throw new IllegalStateException("Trait does not have a main MultiText");
    }

    @Override
    protected void addToMainMultiText(Form t) {
        throw new UnsupportedOperationException(
            "Trait does not have a main MultiText"
        );
    }

    public LiftFieldAndTraitDefinition getDefinition() {
        return definitionProperty.get();
    }

    // Values

    // private SimpleObjectProperty<ZonedDateTime> dateTimeProperty;
    // private StringProperty stringValueProperty;
    // private SimpleObjectProperty<LiftHeaderRangeElement> rangeElementProperty;

    // private SimpleSetProperty<LiftHeaderRangeElement> rangeElementSetProperty;
    // private SimpleListProperty<LiftHeaderRangeElement> rangeElementListProperty;
    // private SimpleIntegerProperty integerProperty;

    public String getValue() {
        return switch (definitionProperty.get().getType().get()) {
            case LiftFieldAndTraitDefinitionDataModel.DATETIME -> this.dateTimeProperty.get().toString();
            case LiftFieldAndTraitDefinitionDataModel.STRING -> this.stringValueProperty.get();
            case LiftFieldAndTraitDefinitionDataModel.OPTION -> this.rangeElementProperty.get().getId();
            case LiftFieldAndTraitDefinitionDataModel.OPTION_COLLECTION -> {
                throw new UnsupportedOperationException("OPTION_COLLECTION type is not supported for trait value");
                // this.rangeElementSetProperty.get().stream()
                //     .map(LiftHeaderRangeElement::getId)
                //     .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionDataModel.OPTION_SEQUENCE -> {
                throw new UnsupportedOperationException("OPTION_SEQUENCE type is not supported for trait value");
                // this.rangeElementListProperty.get().stream()
                //     .map(LiftHeaderRangeElement::getId)
                //  .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionDataModel.INTEGER -> Integer.toString(this.integerProperty.get());
            default -> throw new IllegalArgumentException("Illegal trait type: " + definitionProperty.get().getTypeStr());
        };
    }

    public StringProperty valueProperty() {
        return stringValueProperty;
    }

    public void setValue(String value) {
        if (value == null) value = "";
        //valueProperty.set(value);
        switch (definitionProperty.get().getType().get()) {
            case LiftFieldAndTraitDefinitionDataModel.DATETIME -> this.dateTimeProperty.set(ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME));
            case LiftFieldAndTraitDefinitionDataModel.STRING -> this.stringValueProperty.set(value);
            case LiftFieldAndTraitDefinitionDataModel.OPTION -> throw new UnsupportedOperationException("OPTION type is not supported for trait value");
            case LiftFieldAndTraitDefinitionDataModel.OPTION_COLLECTION -> throw new UnsupportedOperationException("OPTION_COLLECTION type is not supported for trait value");
            case LiftFieldAndTraitDefinitionDataModel.OPTION_SEQUENCE -> throw new UnsupportedOperationException("OPTION_SEQUENCE type is not supported for trait value");
            case LiftFieldAndTraitDefinitionDataModel.INTEGER -> this.integerProperty.set(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Illegal trait type: " + definitionProperty.get().getTypeStr());
        }
    }

    // Parent

    protected void setParent(HasTrait parent) {
        if (parent == null) throw new IllegalArgumentException("Parent is null");
        this.parent = parent;
    }

    // Annotations

    @Override
    public void addAnnotation(LiftAnnotation a) {
        this.annotations.add(a);
        a.setParent(this);
    }

    public List<LiftAnnotation> getAnnotations() {
        return annotations;
    }

    public HasTrait getParent() {
        return parent;
    }

}
