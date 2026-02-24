package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    protected Optional<String> type = Optional.empty();
    @Setter protected AbstractNotable parent;

    private final StringProperty typeProperty = new SimpleStringProperty(this, "type", "");

    protected LiftNote() {
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    public Optional<String> getType() {
        return type;
    }

    public void setType(String type) {
        this.type = Optional.of(type);
        this.typeProperty.set(type);
    }

    public StringProperty typeProperty() {
        return typeProperty;
    }

    public AbstractNotable getParent() {
        return parent;
    }
}
