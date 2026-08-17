package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class LiftRelation
    extends AbstractExtensibleWithField
    implements HasType, HasRefId
{

    /** Relation type (e.g. lexical-relation value from header ranges). Mutable for UI editing. */

    protected Optional<String> refId = Optional.empty();

    protected HasRelations parent;

    public HasRelations getParent() {
        return parent;
    }

    protected Optional<Integer> order = Optional.empty();

    public Optional<Integer> getOrder() {
        return order;
    }

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty;
    private final ObjectProperty<AbstractIdentifiable> refObjectProperty;

    public LiftRelation(LiftHeaderRangeElement type) {
        this();
        this.typeProperty.set(type);
    }

    public LiftRelation() {
        this.typeProperty = new SimpleObjectProperty<>(this, "type", null);
        this.refObjectProperty = new SimpleObjectProperty<AbstractIdentifiable>(this, "refObject", null);
    }

    protected void setParent(HasRelations parent) {
        this.parent = parent;
    }

    @Override
    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
    }

    /** Updates the relation type and the bound JavaFX property. */
    public void setType(LiftHeaderRangeElement newType) {
        typeProperty.set(newType);
    }

    public Optional<String> getRefID() {
        return refId;
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }

    @Override
    public Optional<String> getRefId() {
        return this.refId;
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return this.refObjectProperty.get();
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        this.refObjectProperty.set(refObject);
    }

    public ObjectProperty<LiftHeaderRangeElement> typeProperty() {
        return typeProperty;
    }

    public static LiftRelation create(LiftHeaderRangeElement type) {
        return new LiftRelation(type);
    }

    public static LiftRelation create() {
        return new LiftRelation();
    }
}
