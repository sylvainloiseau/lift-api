package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

public final class LiftRelation
    extends AbstractExtensibleWithField
    implements HasType, HasRefId
{

    protected HasRelations parent;

    private final TrackedProperty<Feature> typeProperty;

    // Plain fields until a property is asked for (see AbstractExtensibleWithoutField).
    private String refId;
    private TrackedProperty<String> refIdProperty;
    private AbstractIdentifiable refObject;
    private TrackedProperty<AbstractIdentifiable> refObjectProperty;
    private Integer order;
    private TrackedProperty<Integer> orderProperty;

    public LiftRelation(Feature type) {
        this.typeProperty = new TrackedProperty<>(this, ModelProperty.TYPE, type);
    }

    public LiftRelation() {
        this(null);
    }

    public HasRelations getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftRelation.
     *
     * @param parent the new parent, or {@code null} when detaching this relation
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(HasRelations parent) {
        this.parent = parent;
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Change the type of this relation, through
     * {@link HasRelations#retypeRelation(LiftRelation, Feature)} when it has a parent.
     */
    @Override
    public void setType(Feature newType) {
        if (parent != null) {
            parent.retypeRelation(this, newType);
        } else {
            assignType(newType);
        }
    }

    /**
     * Write the type. Called by {@link #setType(Feature)} when this component has no
     * parent, and otherwise by the parent's {@code retype} method once it has checked
     * and re-keyed what depends on the type.
     */
    void assignType(Feature newType) {
        typeProperty.set(newType);
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    public Optional<Integer> getOrder() {
        return Optional.ofNullable(orderProperty != null ? orderProperty.get() : order);
    }

    /**
     * @param order the {@code order} attribute, or {@code null} for none
     */
    public void setOrder(Integer order) {
        if (orderProperty != null) {
            orderProperty.set(order);
        } else {
            Integer old = this.order;
            this.order = order;
            Changes.valueChanged(this, ModelProperty.ORDER, old, order);
        }
    }

    /** The {@code order} attribute; {@code null} when there is none. */
    public ObjectProperty<Integer> orderProperty() {
        if (orderProperty == null) {
            orderProperty = new TrackedProperty<>(this, ModelProperty.ORDER, order);
            order = null;
        }
        return orderProperty;
    }

    /**
     * Set the LIFT id this relation refers to. On a registered relation, the
     * dictionary's reference counts follow.
     *
     * @param refId the id, or {@code null} for none
     */
    public void setRefId(String refId) {
        String oldTarget = References.target(this);
        if (refIdProperty != null) {
            refIdProperty.set(refId);
        } else {
            String old = this.refId;
            this.refId = refId;
            Changes.valueChanged(this, ModelProperty.REF_ID, old, refId);
        }
        References.retarget(this, oldTarget);
    }

    /** @deprecated same as {@link #getRefId()} */
    @Deprecated(forRemoval = true)
    public Optional<String> getRefID() {
        return getRefId();
    }

    @Override
    public Optional<String> getRefId() {
        return Optional.ofNullable(refIdProperty != null ? refIdProperty.get() : refId);
    }

    /** The referred LIFT id, read-only: it changes through {@link #setRefId(String)}. */
    public ReadOnlyObjectProperty<String> refIdProperty() {
        if (refIdProperty == null) {
            refIdProperty = new TrackedProperty<>(this, ModelProperty.REF_ID, refId);
            refId = null;
        }
        return refIdProperty.getReadOnlyProperty();
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return refObjectProperty != null ? refObjectProperty.get() : refObject;
    }

    /**
     * Set the entry or sense this relation refers to. On a registered relation, the
     * dictionary's reference counts follow.
     *
     * @param refObject the referred component, or {@code null} for none
     */
    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        String oldTarget = References.target(this);
        if (refObjectProperty != null) {
            refObjectProperty.set(refObject);
        } else {
            AbstractIdentifiable old = this.refObject;
            this.refObject = refObject;
            Changes.valueChanged(this, ModelProperty.REF_OBJECT, old, refObject);
        }
        References.retarget(this, oldTarget);
    }

    /** The referred component, read-only: it changes through {@link #setRefObject}. */
    public ReadOnlyObjectProperty<AbstractIdentifiable> refObjectProperty() {
        if (refObjectProperty == null) {
            refObjectProperty = new TrackedProperty<>(this, ModelProperty.REF_OBJECT, refObject);
            refObject = null;
        }
        return refObjectProperty.getReadOnlyProperty();
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public static LiftRelation create(Feature type) {
        return new LiftRelation(type);
    }

    public static LiftRelation create() {
        return new LiftRelation();
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }
}
