package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

public final class LiftRelation
    extends AbstractExtensibleWithField
    implements HasType, HasRefId
{

    protected Optional<String> refId = Optional.empty();

    protected HasRelations parent;

    public HasRelations getParent() {
        return parent;
    }

    protected Optional<Integer> order = Optional.empty();

    public Optional<Integer> getOrder() {
        return order;
    }

    private final ReadOnlyObjectWrapper<Feature> typeProperty;

    private final ObjectProperty<AbstractIdentifiable> refObjectProperty;

    public LiftRelation(Feature type) {
        this();
        this.typeProperty.set(type);
    }

    public LiftRelation() {
        this.typeProperty = new ReadOnlyObjectWrapper<>(this, "type", null);
        this.refObjectProperty = new SimpleObjectProperty<AbstractIdentifiable>(this, "refObject", null);
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

    public void setRefId(String refId) {
        this.refId = Optional.of(refId);
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

    public Optional<String> getRefID() {
        return refId;
    }

    public MultiText getUsage() {
        return getMainMultiText();
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }

    @Override
    public Optional<String> getRefId() {
        return this.refId;
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return this.refObjectProperty.get();
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        this.refObjectProperty.set(refObject);
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
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
