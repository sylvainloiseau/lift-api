package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


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

    private LiftReversal main;

    final ObservableList<LiftReversal> reversals = FXCollections.observableArrayList();
    private ObservableList<LiftReversal> reversalsView;

    protected HasReversal parent;

    private final TrackedProperty<Feature> typeProperty;

    public LiftReversal() {
        this(null);
    }

    public LiftReversal(Feature type) {
        this.typeProperty = new TrackedProperty<>(this, ModelProperty.TYPE, type);
    }

    public HasReversal getParent() {
        return parent;
    }

    @Override
    public void addReversal(LiftReversal reversal) {
        addReversal(reversals.size(), reversal);
    }

    @Override
    public void addReversal(int index, LiftReversal reversal) {
        reversals.add(index, reversal);
        reversal.setParent(this);
        adopted(reversal);
    }

    @Override
    public ObservableList<LiftReversal> getReversals() {
        if (reversalsView == null) reversalsView = FXCollections.unmodifiableObservableList(reversals);
        return reversalsView;
    }

    /** @deprecated use {@link #getReversals()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftReversal> reversalsProperty() {
        return new ReadOnlyListWrapper<>(this, "reversals", getReversals()).getReadOnlyProperty();
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
            reversal != null && (reversals.contains(reversal) || main == reversal)
        );
        reversal.assignType(type);
    }

    public LiftReversal getMain() {
        return main;
    }

    /**
     * Set the {@code <main>} of this reversal, in place of the current one, which is
     * deleted (and unregistered) first.
     *
     * The back reference was missing here, which left the nested reversal unreachable
     * from its own parent even though {@code childrenOf} reaches it from above: it
     * could be registered but never detached.
     *
     * @param main the new main, or {@code null} to delete the current one
     */
    public void setMain(LiftReversal main) {
        if (main == this.main) {
            return;
        }
        deleteMain();
        if (main != null) {
            this.main = main;
            main.setParent(this);
            adopted(main);
        }
    }

    /** Empty the {@code <main>} slot (used when unlinking the main). */
    void clearMain() {
        this.main = null;
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
        requireChild(reversal, reversals.contains(reversal));
        removeChild(reversal);
    }

    /**
     * Remove the {@code <main>} of this reversal, unregistering it if this reversal
     * belongs to a dictionary. Does nothing if there is none.
     */
    public void deleteMain() {
        if (main != null) {
            removeChild(main);
        }
    }
}
