package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

/**
 * A reversal entry associated with a sense.
 *
 * In LIFT, a {@code <reversal>} element appears inside a {@code <sense>}.
 * It contains:
 * <ul>
 *   <li>An optional {@code @type} attribute</li>
 *   <li>A multitext (forms in one or several languages)</li>
 *   <li>An optional recursive {@code <main>} sub-element (itself a reversal-main)</li>
 * </ul>
 *
 * @see LiftSense
 */
public final class LiftReversal
    extends AbstractLiftRoot
    implements HasType, HasReversal
{

    protected LiftReversal main;

    protected final ListProperty<LiftReversal> reversalsProperty =
        new SimpleListProperty<>(
            this,
            "reversals",
            FXCollections.observableArrayList()
        );

    protected HasReversal parent;

    public HasReversal getParent() {
        return parent;
    }

    private final ReadOnlyObjectWrapper<Feature> typeProperty = new ReadOnlyObjectWrapper<>(
        this,
        "type",
        null
    );

    public LiftReversal() {
    }

    public LiftReversal(Feature type) {
        this.typeProperty.set(type);
    }


    public void addReversal(LiftReversal reversal) {
        reversalsProperty.add(reversal);
        reversal.setParent(this);
        adopted(reversal);
    }

    public List<LiftReversal> getReversals() {
        return reversalsProperty.get();
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Change the type of this reversal, through
     * {@link HasReversal#retypeReversal(LiftReversal, Feature)} when it has a parent.
     */
    @Override
    public void setType(Feature type) {
        if (parent != null) {
            parent.retypeReversal(this, type);
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
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
    }

    /**
     * Change the type of a reversal held by this one, either in its list of reversals
     * or as its {@code <main>}.
     */
    @Override
    public void retypeReversal(LiftReversal reversal, Feature type) {
        requireChild(
            reversal,
            reversal != null && (reversalsProperty.contains(reversal) || main == reversal)
        );
        reversal.assignType(type);
    }

    public LiftReversal getMain() {
        return main;
    }

    /**
     * Set the {@code <main>} of this reversal.
     *
     * The back reference was missing here, which left the nested reversal unreachable
     * from its own parent even though {@code childrenOf} reaches it from above: it
     * could be registered but never detached.
     */
    public void setMain(LiftReversal main) {
        this.main = main;
        if (main != null) {
            main.setParent(this);
            adopted(main);
        }
    }

    @Override
    public ReadOnlyObjectProperty<Feature> typeProperty() {
        return typeProperty.getReadOnlyProperty();
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftReversal.
     * 
     * @param parent the new parent, or {@code null} when detaching this reversal
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(HasReversal parent) {
        this.parent = parent;
    }


    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a nested reversal, unregistering it - and everything under it - if this
     * reversal belongs to a dictionary.
     *
     * @param reversal a reversal held by this one
     */
    @Override
    public void deleteReversal(LiftReversal reversal) {
        requireChild(reversal, reversalsProperty.contains(reversal));
        orphaned(reversal);
        reversal.detach();
    }

    /**
     * Remove the {@code <main>} of this reversal, unregistering it if this reversal
     * belongs to a dictionary. Does nothing if there is none.
     */
    public void deleteMain() {
        if (main == null) {
            return;
        }
        LiftReversal removed = main;
        orphaned(removed);
        removed.detach();
    }
}
