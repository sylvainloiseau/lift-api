package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.ArrayList;

public final class GrammaticalInfo
    implements HasTrait {

    protected LiftHeaderRangeElement value;

    protected final List<LiftTrait> traits = new ArrayList<>();

    protected GrammaticalInfo(LiftHeaderRangeElement v) {
        this.value = v;
    }

    public LiftHeaderRangeElement getGramInfoValue() {
        return this.value;
    }

    public String getValue() {
        return value.getId();
    }

    @Override
    public void addTrait(LiftTrait t) {
        traits.add(t);
        t.setParent(this);
    }

    @Override
    public List<LiftTrait> getTraits() {
        return traits;
    }

}
