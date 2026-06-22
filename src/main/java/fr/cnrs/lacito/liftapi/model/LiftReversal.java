package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import lombok.Getter;

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

    protected Optional<String> type = Optional.empty();
    protected LiftReversal main;
    protected final ListProperty<LiftReversal> reversalsProperty =
        new SimpleListProperty<>(
            this,
            "reversals",
            FXCollections.observableArrayList()
        );

    @Getter
    protected HasReversal parent;

    private final StringProperty typeProperty = new SimpleStringProperty(
        this,
        "type",
        ""
    );

    public LiftReversal() {}

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

    public Optional<String> getType() {
        return type;
    }

    public void setType(String type) {
        this.type = Optional.of(type);
        this.typeProperty.set(type);
    }

    public LiftReversal getMain() {
        return main;
    }

    public void setMain(LiftReversal main) {
        this.main = main;
    }

    public StringProperty typeProperty() {
        return typeProperty;
    }

    protected void setParent(HasReversal parent) {
        this.parent = parent;
    }
}
