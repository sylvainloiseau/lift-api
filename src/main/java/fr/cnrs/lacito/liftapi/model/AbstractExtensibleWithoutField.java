package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public abstract sealed class AbstractExtensibleWithoutField
    extends AbstractLiftRoot
    implements ExtensibleWithoutField, HasAnnotation, HasTrait
    permits AbstractExtensibleWithField, LiftField {

    protected Optional<String> dateCreated = Optional.empty();
    protected Optional<String> dateModified = Optional.empty();
    protected final ListProperty<LiftAnnotation> annotationsProperty =
            new SimpleListProperty<>(this, "annotations", FXCollections.observableArrayList());
    protected final ListProperty<LiftTrait> traitsProperty =
            new SimpleListProperty<>(this, "traits", FXCollections.observableArrayList());

    public void addTrait(LiftTrait t) {
        traitsProperty.add(t);
        t.setParent(this);
    }

    public void addAnnotation(LiftAnnotation a) {
        annotationsProperty.add(a);
        a.setParent(this);
    }

    public void setDateCreated(String value) {
        this.dateCreated = Optional.ofNullable(value);
    }

    public void setDateModified(String value) {
        this.dateModified = Optional.ofNullable(value);
    }

    public Optional<String> getDateCreated() {
        return dateCreated;
    }

    public Optional<String> getDateModified() {
        return dateModified;
    }

    public List<LiftAnnotation> getAnnotations() {
        return annotationsProperty.get();
    }

    public List<LiftTrait> getTraits() {
        return traitsProperty.get();
    }

    public ListProperty<LiftAnnotation> annotationsProperty() {
        return annotationsProperty;
    }

    public ListProperty<LiftTrait> traitsProperty() {
        return traitsProperty;
    }
}
