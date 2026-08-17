package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.ArrayList;

public final class GrammaticalInfo
    implements HasTrait {

    protected String value;
    protected final List<LiftTrait> traits = new ArrayList<>();

    protected GrammaticalInfo(String v) {
        this.value = v;
    }

    public String getGramInfoValue() {
        return this.value;
    }

    @Override
    public void addTrait(LiftTrait t) {
        traits.add(t);
        t.setParent(this);
    }

    public String getValue() {
        return value;
    }

    public List<LiftTrait> getTraits() {
        return traits;
    }

}
