package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    private final ObjectProperty<Feature> typeProperty = new SimpleObjectProperty<>(
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

    @Override
    public void setType(Feature type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
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

    public ObjectProperty<Feature> typeProperty() {
        return typeProperty;
    }

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
