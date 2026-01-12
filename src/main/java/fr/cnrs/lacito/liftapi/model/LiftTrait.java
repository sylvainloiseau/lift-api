package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * A trait is a key-value pair. The key doesn't have to be unique on the object that receive the field.
 */
public final class LiftTrait
    extends AbstractLiftRoot
    implements HasAnnotation {
    
    @Getter final String name;
    @Getter final String value;
    @Getter protected final List<LiftAnnotation> annotations = new ArrayList<>();
    protected HasTrait parent;

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
