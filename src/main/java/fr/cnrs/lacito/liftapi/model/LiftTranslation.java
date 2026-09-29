package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * A translation of a {@link LiftExample}, of a given type (LIFT {@code <translation>}).
 *
 * A translation is a component of its own, rather than a bare {@link MultiText} in a
 * map of its example, so that it follows the same rules as every other component: it
 * is registered when it joins an attached example ({@link LiftExample#addTranslation})
 * and unregistered when it is deleted from it
 * ({@link LiftExample#deleteTranslation}), and its text is counted towards the
 * dictionary's meta-languages. See the {@code package-info} of this package.
 *
 * The type is the key under which the example holds this translation. Changing it
 * with {@link #setType(Feature)} therefore goes through
 * {@link LiftExample#retypeTranslation(LiftTranslation, Feature)}, which re-keys the
 * translation and refuses a type the example already has.
 */
public final class LiftTranslation extends AbstractLiftRoot implements HasType {

    private final ReadOnlyObjectWrapper<Feature> typeProperty =
        new ReadOnlyObjectWrapper<>(this, "type", null);

    private LiftExample parent;

    private LiftTranslation(Feature type) {
        assignType(type);
    }

    /**
     * Create a detached translation.
     *
     * @param type the translation type, an element of the header's
     *        {@code translation-type} range
     */
    public static LiftTranslation create(Feature type) {
        return new LiftTranslation(type);
    }

    /**
     * @return the text of this translation, in one or several meta-languages
     */
    public MultiText getTranslation() {
        return getMainMultiText();
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Change the type of this translation.
     *
     * @throws DuplicateTypeException if the example already has a translation of that
     *         type
     */
    @Override
    public void setType(Feature type) {
        if (parent != null) {
            parent.retypeTranslation(this, type);
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
        if (type == null) {
            throw new IllegalArgumentException("Translation type cannot be null");
        }
        typeProperty.set(type);
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    public LiftExample getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftTranslation.
     *
     * @param parent the new parent, or {@code null} when detaching this translation
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(LiftExample parent) {
        this.parent = parent;
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
