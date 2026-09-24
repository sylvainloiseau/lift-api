package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * A Lift note (not to be confused with {@link LiftTrait}, {@link LiftAnnotation}, {@link LiftField}; for comparison see {@link LiftTrait}).
 * 
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

    private final ObjectProperty<Feature> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    public LiftNote() {}

    public LiftNote(Feature element) {
        typeProperty.set(element);
    }

    // Text -----------------------------------

    public MultiText getText() {
        return getMainMultiText();
    }

    public void addText(Form f) {
        getText().add(f);
    }

    // Parent -----------------------------------

    public AbstractNotable getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftNote.
     * 
     * @param parent the new parent, or {@code null} when detaching this note
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(AbstractNotable parent) {
        this.parent = parent;
    }

    // Type -----------------------------------

    @Override
    public void setType(Feature type) {
        if (type == null) throw new IllegalArgumentException("note type cannot be null");
        this.typeProperty.set(type);
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    public ObjectProperty<Feature> typeProperty() {
        return typeProperty;
    }

    public static LiftNote create() {
        return new LiftNote();
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
