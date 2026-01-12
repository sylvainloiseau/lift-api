package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

public final class LiftRelation
    extends AbstractExtensibleWithField {
        
    @Getter protected final String type;
    @Getter protected Optional<String> refID = Optional.empty();
    @Getter protected Optional<Integer> order = Optional.empty();
    @Getter @Setter protected AbstractExtensibleWithoutField parent;

    protected LiftRelation(String type) {
        this.type = type;
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public void setRefID(String refID) {
        this.refID = Optional.of(refID);
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }

    public void setRefId(String value) {
        refID = Optional.of(value);
    }
}
