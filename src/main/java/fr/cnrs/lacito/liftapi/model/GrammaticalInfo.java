package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.ArrayList;

/**
 * A component containing a reference to part of speech together with {@link LiftTrait}, to be registered to a {@link LiftSense}.
 * 
 * The part of speech value is a reference towards one
 * of the {@link Feature}; the list of part of speech value is managed through
 * the {@link LiftHeader#getGrammaticalInfoManager()}.
 * 
 * GrammaticalInfo
 */
public final class GrammaticalInfo
    implements HasTrait {

    protected Feature value;

    protected final List<LiftTrait> traits = new ArrayList<>();

    protected GrammaticalInfo(Feature v) {
        this.value = v;
    }

    public Feature getGramInfoValue() {
        return this.value;
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
