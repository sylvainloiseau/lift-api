package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

/**
 * A note contains a Multitext and has a type. Eg :
 * 
 * <pre>
 * &lt;sense id="582795c9-9350-4e3b-af34-b72e9b5c89aa">
 * &lt;!-- ... -->
 * &lt;note type="source">
 * &lt;form lang="en">&lt;text>2014.VI.87&lt;/text>&lt;/form>
 * &lt;/note>
 * &lt;!-- ... -->
 * &lt;/sense>
 * </pre>
 * 
 * @see HasNote
 */
public final class LiftNote extends AbstractExtensibleWithField {

    @Getter protected Optional<String> type = Optional.empty();
    @Setter protected AbstractNotable parent;

    protected LiftNote() {
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    protected void setType(String type) {
        this.type = Optional.of(type);
    }

}
