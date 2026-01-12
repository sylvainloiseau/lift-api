package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.ArrayList;
import lombok.Getter;

public final class GrammaticalInfo
    implements HasTrait {

    @Getter protected String value;
    @Getter protected final List<LiftTrait> traits = new ArrayList<>();

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
}
