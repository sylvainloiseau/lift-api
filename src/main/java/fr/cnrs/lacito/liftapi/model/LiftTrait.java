package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A trait is a key-value pair. The key doesn't have to be unique on the object that receive the field.
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


    private ReadOnlyStringWrapper nameProperty;
    private StringProperty valueProperty;

    public LiftTrait(String name, String value) {
        this.nameProperty = new ReadOnlyStringWrapper(this, "name", name);
        this.valueProperty = new SimpleStringProperty(this, "value", value);
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, String value) {
        //TODO Auto-generated constructor stub
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, LiftHeaderRangeElement e) {
        //TODO Auto-generated constructor stub
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, HashSet<LiftHeaderRangeElement> set) {
        //TODO Auto-generated constructor stub
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, List<LiftHeaderRangeElement> elements) {
        //TODO Auto-generated constructor stub
    }

    public LiftTrait(LiftFieldAndTraitDefinition def, Integer v) {
        //TODO Auto-generated constructor stub
    }

    @Override
    public MultiText getMainMultiText() {
        throw new IllegalStateException("Trait does not have a main MultiText");
    }

    @Override
    protected void addToMainMultiText(Form t) {
        throw new UnsupportedOperationException(
            "Trait does not support adding to main MultiText"
        );
    }

    public ReadOnlyStringProperty nameProperty() {
        return nameProperty.getReadOnlyProperty();
    }

    public String getName() {
        return nameProperty.get();
    }

    public String getValue() {
        return valueProperty.get();
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public void setValue(String value) {
        if (value == null) value = "";
        valueProperty.set(value);
    }

    protected void setParent(HasTrait parent) {
        this.parent = parent;
    }

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

    public static LiftTrait create(String name, String value) {
        // TODO parse name
        return new LiftTrait(name, value);
    }
}
