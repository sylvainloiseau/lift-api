package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;

/**
 * A trait is a key-value pair. The key doesn't have to be unique on the object that receive the field.
 */
public final class LiftTrait
    extends AbstractLiftRoot
    implements HasAnnotation {
    
    @Getter final String name;
    @Getter private String value;
    @Getter protected final List<LiftAnnotation> annotations = new ArrayList<>();
    protected HasTrait parent;

    private final ReadOnlyStringWrapper nameProperty;
    private final StringProperty valueProperty;
    private boolean syncingFromProperty = false;
    private boolean syncingFromModel = false;

    @Override
    public MultiText getMainMultiText() {
        throw new IllegalStateException("Trait does not have a main MultiText");
    }

    @Override
    protected void addToMainMultiText(Form t) {
        throw new UnsupportedOperationException("Trait does not support adding to main MultiText");
    }

    protected LiftTrait(String name, String value) {
        this.name = name;
        this.value = value;
        this.nameProperty = new ReadOnlyStringWrapper(this, "name", name);
        this.valueProperty = new SimpleStringProperty(this, "value", value);
        this.valueProperty.addListener((obs, oldV, newV) -> {
            if (syncingFromModel) return;
            syncingFromProperty = true;
            try {
                setValue(newV);
            } finally {
                syncingFromProperty = false;
            }
        });
    }

    public ReadOnlyStringProperty nameProperty() {
        return nameProperty.getReadOnlyProperty();
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        if (!syncingFromProperty && valueProperty != null) {
            if (!this.value.equals(valueProperty.get())) {
                syncingFromModel = true;
                try {
                    valueProperty.set(this.value);
                } finally {
                    syncingFromModel = false;
                }
            }
        }
    }

    protected void setParent(HasTrait parent) {
        this.parent = parent;
    }

    @Override
    public void addAnnotation(LiftAnnotation a) {
        this.annotations.add(a);
        a.setParent(this);
    }

    public HasTrait getParent() {
        return parent;
    }

}
