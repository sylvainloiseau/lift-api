package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

public final class LiftHeaderFieldDefinition extends AbstractLiftRoot {

    @Getter final String name;
    final LiftHeader parent;
    
    @Getter @Setter Optional<String>  fClass = Optional.empty();
    @Getter @Setter Optional<String>  type = Optional.empty();
    @Getter @Setter Optional<String>  optionRange = Optional.empty();
    @Getter @Setter Optional<String>  writingSystem = Optional.empty();

    @Getter MultiText label = new MultiText();

    protected LiftHeaderFieldDefinition(String tag, LiftHeader parent) {
        this.name = tag;
        this.parent = parent;
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

}
