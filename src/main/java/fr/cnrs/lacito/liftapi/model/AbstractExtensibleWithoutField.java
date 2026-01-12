package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract sealed class AbstractExtensibleWithoutField
    extends AbstractLiftRoot
    implements ExtensibleWithoutField, HasAnnotation, HasTrait
    permits AbstractExtensibleWithField, LiftField {

    protected Optional<String> dateCreated = Optional.empty();
    protected Optional<String> dateModified = Optional.empty();
    protected final List<LiftAnnotation> annotations = new ArrayList<>();
    protected final List<LiftTrait> traits = new ArrayList<>();

    public void addTrait(LiftTrait t) {
        traits.add(t);
        t.setParent(this);
    }

    public void addAnnotation(LiftAnnotation a) {
        annotations.add(a);
        a.setParent(this);
    }

    protected void setDateCreated(String value) {
        this.dateCreated = Optional.of(value);
    }

    protected void setDateModified(String value) {
        this.dateModified = Optional.of(value);
    }

    public Optional<String> getDateCreated() {
        return dateCreated;
    }

    public Optional<String> getDateModified() {
        return dateModified;
    }

    public List<LiftAnnotation> getAnnotations() {
        return annotations;
    }

    public List<LiftTrait> getTraits() {
        return traits;
    }

}
