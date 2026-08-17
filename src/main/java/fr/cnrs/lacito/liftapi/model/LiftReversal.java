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

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    public LiftReversal(LiftHeaderRangeElement type) {
        this.typeProperty.set(type);
    }

    public void addReversal(LiftReversal reversal) {
        reversalsProperty.add(reversal);
        reversal.setParent(this);
    }

    public List<LiftReversal> getReversals() {
        return reversalsProperty.get();
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
    }

    public void setType(LiftHeaderRangeElement type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
    }

    public LiftReversal getMain() {
        return main;
    }

    public void setMain(LiftReversal main) {
        this.main = main;
    }

    public ObjectProperty<LiftHeaderRangeElement> typeProperty() {
        return typeProperty;
    }

    protected void setParent(HasReversal parent) {
        this.parent = parent;
    }
}
