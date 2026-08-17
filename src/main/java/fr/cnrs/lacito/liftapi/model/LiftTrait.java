package fr.cnrs.lacito.liftapi.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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

    private SimpleObjectProperty<ZonedDateTime> dateTimeProperty;
    private StringProperty stringValueProperty;
    private SimpleObjectProperty<LiftHeaderRangeElement> rangeElementProperty;
    private SimpleSetProperty<LiftHeaderRangeElement> rangeElementSetProperty;
    private SimpleListProperty<LiftHeaderRangeElement> rangeElementListProperty;
    private SimpleIntegerProperty integerProperty;

    private SimpleObjectProperty<LiftFieldAndTraitDefinition> definitionProperty;

    public LiftTrait(LiftFieldAndTraitDefinition def) {
        this.definitionProperty = new SimpleObjectProperty<>(this, "definition", def);
        switch (def.getType().get()) {
            case LiftFieldAndTraitDefinitionType.DATETIME ->
                this.dateTimeProperty = new SimpleObjectProperty<>(this, "value", null);
            case LiftFieldAndTraitDefinitionType.STRING ->
                this.stringValueProperty = new SimpleStringProperty(this, "value", null);
            case LiftFieldAndTraitDefinitionType.OPTION ->
                this.rangeElementProperty = new SimpleObjectProperty<>(this, "value", null);
            case LiftFieldAndTraitDefinitionType.OPTION_COLLECTION ->
                this.rangeElementSetProperty = new SimpleSetProperty<>(this, "value", null);
            case LiftFieldAndTraitDefinitionType.OPTION_SEQUENCE ->
                this.rangeElementListProperty = new SimpleListProperty<>(this, "value", null);
            case LiftFieldAndTraitDefinitionType.INTEGER ->
                this.integerProperty = new SimpleIntegerProperty(this, "value");
            default -> throw new IllegalArgumentException("Illegal trait type: " + def.getTypeStr());
        }
    }

//    public LiftTrait(String name, String value) {
//        this.nameProperty = new ReadOnlyStringWrapper(this, "name", name);
//        this.valueProperty = new SimpleStringProperty(this, "value", value);
//    }

    public LiftTrait(LiftFieldAndTraitDefinition def, String value) {
        this(def);
        this.stringValueProperty.set(value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, LiftHeaderRangeElement e) {
        this(def);
        this.rangeElementProperty.set(e);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, HashSet<LiftHeaderRangeElement> set) {
        this(def);
        this.rangeElementSetProperty.addAll(set);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, List<LiftHeaderRangeElement> elements) {
        this(def);
        this.rangeElementListProperty.setAll(elements);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Integer v) {
        this(def);
        this.integerProperty.set(v);
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
