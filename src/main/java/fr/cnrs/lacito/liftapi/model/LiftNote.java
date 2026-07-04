package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

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
public final class LiftNote
    extends AbstractExtensibleWithField
    implements HasType
{

    protected AbstractNotable parent;

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    //public LiftNote() {}

    public LiftNote(LiftHeaderRangeElement element) {
        typeProperty.set(element);
    }

    public MultiText getText() {
        return getMainMultiText();
    }

    protected void setParent(AbstractNotable parent) {
        this.parent = parent;
    }

    @Override
    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
    }

    public void addText(Form f) {
        getText().add(f);
    }

    public void setType(LiftHeaderRangeElement type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
    }

    public ObjectProperty<LiftHeaderRangeElement> typeProperty() {
        return typeProperty;
    }

    public AbstractNotable getParent() {
        return parent;
    }

    public static LiftNote create(LiftHeaderRangeElement element) {
        return new LiftNote(element);
    }

}
