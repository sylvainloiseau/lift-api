package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

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

    private final ReadOnlyObjectWrapper<Feature> typeProperty = new ReadOnlyObjectWrapper<>(
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

    /**
     * Change the type of this note. When the note belongs to a component, the change
     * goes through {@link AbstractNotable#retypeNote(LiftNote, Feature)}, which re-keys
     * the note and refuses a type another note of that component already has.
     *
     * @throws DuplicateTypeException if the parent already has a note of that type
     */
    @Override
    public void setType(Feature type) {
        if (parent != null) {
            parent.retypeNote(this, type);
        } else {
            assignType(type);
        }
    }

    /**
     * Write the type. Called by {@link #setType(Feature)} when this component has no
     * parent, and otherwise by the parent's {@code retype} method once it has checked
     * and re-keyed what depends on the type.
     */
    void assignType(Feature type) {
        if (type == null) throw new IllegalArgumentException("note type cannot be null");
        this.typeProperty.set(type);
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    public static LiftNote create() {
        return new LiftNote();
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
