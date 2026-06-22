package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

public final class LiftRelation
    extends AbstractExtensibleWithField
    implements HasType
{

    /** Relation type (e.g. lexical-relation value from header ranges). Mutable for UI editing. */
    private Optional<String> type = Optional.empty();
    protected Optional<String> refID = Optional.empty();

    @Getter
    protected HasRelations parent;

    @Getter
    protected Optional<Integer> order = Optional.empty();

    private final ReadOnlyStringWrapper typePropertyWrapper;
    private final StringProperty refIdProperty;

    public LiftRelation(String type) {
        this.type = Optional.ofNullable(type);
        this.typePropertyWrapper = new ReadOnlyStringWrapper(
            this,
            "type",
            type
        );
        this.refIdProperty = new SimpleStringProperty(this, "refId", "");
    }

    public LiftRelation() {
        this.typePropertyWrapper = new ReadOnlyStringWrapper(
            this,
            "type",
            type.orElse("")
        );
        this.refIdProperty = new SimpleStringProperty(this, "refId", "");
    }

    protected void setParent(HasRelations parent) {
        this.parent = parent;
    }

    @Override
    public Optional<String> getType() {
        return type;
    }

    /** Updates the relation type and the bound JavaFX property. */
    public void setType(String newType) {
        String v = newType != null ? newType.trim() : "";
        this.type = Optional.of(v);
        typePropertyWrapper.set(v);
    }

    public Optional<String> getRefID() {
        return refID;
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }

    public void setRefId(String value) {
        refID = Optional.of(value);
        refIdProperty.set(value);
    }

    public ReadOnlyStringProperty typeProperty() {
        return typePropertyWrapper.getReadOnlyProperty();
    }

    public StringProperty refIdProperty() {
        return refIdProperty;
    }

    public static LiftRelation create(String type) {
        return new LiftRelation(type);
    }

    public static LiftRelation create() {
        return new LiftRelation();
    }
}
