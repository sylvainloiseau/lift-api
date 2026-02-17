package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

public final class LiftRelation
    extends AbstractExtensibleWithField {
        
    protected final String type;
    protected Optional<String> refID = Optional.empty();
    @Getter protected Optional<Integer> order = Optional.empty();
    @Getter @Setter protected AbstractExtensibleWithoutField parent;

    private final ReadOnlyStringWrapper typePropertyWrapper;
    private final StringProperty refIdProperty;

    protected LiftRelation(String type) {
        this.type = type;
        this.typePropertyWrapper = new ReadOnlyStringWrapper(this, "type", type);
        this.refIdProperty = new SimpleStringProperty(this, "refId", "");
    }

    public String getType() {
        return type;
    }

    public Optional<String> getRefID() {
        return refID;
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public void setRefID(String refID) {
        this.refID = Optional.of(refID);
        this.refIdProperty.set(refID);
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
}
