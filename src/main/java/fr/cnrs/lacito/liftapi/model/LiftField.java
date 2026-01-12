package fr.cnrs.lacito.liftapi.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A Field.
 * 
 * 
 * {@see HasField}.
 */
public final class LiftField
    extends AbstractExtensibleWithoutField {

    @Getter protected final String name;
    @Setter @Getter protected AbstractExtensibleWithField parent;

    protected LiftField(String name) {
        this.name = name;
    }

    public MultiText getText() {
        return getMainMultiText();
    }


}
