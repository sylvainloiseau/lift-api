package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

public final class LiftRelation
    extends AbstractExtensibleWithField
    implements HasType, HasRefId
{

    /** Relation type (e.g. lexical-relation value from header ranges). Mutable for UI editing. */

    protected Optional<String> refId = Optional.empty();

    @Getter
    protected HasRelations parent;

    @Getter
    protected Optional<Integer> order = Optional.empty();

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty;
    private final StringProperty refIdProperty;

    public LiftRelation(LiftHeaderRangeElement type) {
        this.typeProperty = new SimpleObjectProperty<>(
            this,
            "type",
            type
        );
        this.refIdProperty = new SimpleStringProperty(this, "refId", "");
    }

    public LiftRelation() {
        this.typeProperty = new SimpleObjectProperty<>(
            this,
            "type",
            null
        );
        this.refIdProperty = new SimpleStringProperty(this, "refId", "");
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
    public void setRefId(String refId) {
        this.refId = Optional.of(refId);
        refIdProperty.set(refId);
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRefObject'"
        );
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setRefObject'"
        );
    }

    public ObjectProperty<LiftHeaderRangeElement> typeProperty() {
        return typeProperty;
    }

    public StringProperty refIdProperty() {
        return refIdProperty;
    }

    public static LiftRelation create(LiftHeaderRangeElement type) {
        return new LiftRelation(type);
    }

    public static LiftRelation create() {
        return new LiftRelation();
    }
}
