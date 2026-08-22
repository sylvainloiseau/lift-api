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
 * A trait is a key-value pair. The key doesn't have to be unique on the object that receive the trait.
 *
 *
 * A trait is simply a reference to a single range-element in a range. It can be used to give the
dialect for a variant or the status of an entry. The semantics of a trait in a particular context
are given by the parent object and also by the range and range-element being referred to.
Where no range is linked the name is informal or resolved by its use in a field-definition. (Lift specification, p. 13)
 *
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
    private SimpleObjectProperty<LiftHeaderRangeElement> rangeElementProperty;
    private SimpleSetProperty<LiftHeaderRangeElement> rangeElementSetProperty;
    private SimpleListProperty<LiftHeaderRangeElement> rangeElementListProperty;
    private SimpleIntegerProperty integerProperty;

    public LiftTrait(LiftFieldAndTraitDefinition def) {
        this.definitionProperty = new SimpleObjectProperty<>(this, "definition", def);
        switch (def.getDefinitionType().get()) {
            case STRING -> this.stringValueProperty = new SimpleStringProperty(this, "value", "");
            case INTEGER -> this.integerProperty = new SimpleIntegerProperty(this, "value", 0);
            case DATETIME -> this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", null);
            case OPTION -> this.rangeElementProperty = new SimpleObjectProperty<>(this, "value", null);
            case OPTION_COLLECTION -> this.rangeElementListProperty = new SimpleListProperty<>(this, "value", null);
            case OPTION_SEQUENCE -> this.rangeElementSetProperty = new SimpleSetProperty<>(this, "value", null);
            default -> throw new IllegalArgumentException("Unknown definition type: " + def.getDefinitionType().get());
        }
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, String value) {
        this(def);
        this.stringValueProperty = new SimpleStringProperty(this, "value", value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, ZonedDateTime value) {
        this(def);
        this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Integer i) {
        this(def);
        this.integerProperty = new SimpleIntegerProperty(this, "value", i);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, LiftHeaderRangeElement rangeElement) {
        this(def);
        this.rangeElementProperty = new SimpleObjectProperty<>(this, "value", rangeElement);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, HashSet<LiftHeaderRangeElement> rangeElementSet) {
        this(def);
        this.rangeElementSetProperty = new SimpleSetProperty<LiftHeaderRangeElement>(this, "value", FXCollections.observableSet(rangeElementSet));
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, List<LiftHeaderRangeElement> rangeElementList) {
        this(def);
        this.rangeElementListProperty = new SimpleListProperty<LiftHeaderRangeElement>(this, "value", FXCollections.observableList(rangeElementList));
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
            case LiftFieldAndTraitDefinitionType.DATETIME -> this.dateTimeProperty.get().toString();
            case LiftFieldAndTraitDefinitionType.STRING -> this.stringValueProperty.get();
            case LiftFieldAndTraitDefinitionType.OPTION -> this.rangeElementProperty.get().getId();
            case LiftFieldAndTraitDefinitionType.OPTION_COLLECTION -> {
                throw new UnsupportedOperationException("OPTION_COLLECTION type is not supported for trait value");
                // this.rangeElementSetProperty.get().stream()
                //     .map(LiftHeaderRangeElement::getId)
                //     .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionType.OPTION_SEQUENCE -> {
                throw new UnsupportedOperationException("OPTION_SEQUENCE type is not supported for trait value");
                // this.rangeElementListProperty.get().stream()
                //     .map(LiftHeaderRangeElement::getId)
                //  .collect(Collectors.joining(", "));
            }
            case LiftFieldAndTraitDefinitionType.INTEGER -> Integer.toString(this.integerProperty.get());
            default -> throw new IllegalArgumentException("Illegal trait type: " + definitionProperty.get().getTypeStr());
        };
    }

    public StringProperty valueProperty() {
        throw new UnsupportedOperationException("valueProperty is not supported for trait type: " + definitionProperty.get().getTypeStr());
    }

    public void setValue(String value) {
        if (value == null) value = "";
        //valueProperty.set(value);
        switch (definitionProperty.get().getType().get()) {
            case LiftFieldAndTraitDefinitionType.DATETIME -> this.dateTimeProperty.set(ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME));
            case LiftFieldAndTraitDefinitionType.STRING -> this.stringValueProperty.set(value);
            case LiftFieldAndTraitDefinitionType.OPTION -> throw new UnsupportedOperationException("OPTION type is not supported for trait value");
            case LiftFieldAndTraitDefinitionType.OPTION_COLLECTION -> throw new UnsupportedOperationException("OPTION_COLLECTION type is not supported for trait value");
            case LiftFieldAndTraitDefinitionType.OPTION_SEQUENCE -> throw new UnsupportedOperationException("OPTION_SEQUENCE type is not supported for trait value");
            case LiftFieldAndTraitDefinitionType.INTEGER -> this.integerProperty.set(Integer.parseInt(value));
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
