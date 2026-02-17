package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
    extends AbstractLiftRoot {

    protected Optional<String> type = Optional.empty();
    protected LiftReversal main;

    private final StringProperty typeProperty = new SimpleStringProperty(this, "type", "");

    protected LiftReversal() {
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    public Optional<String> getType() {
        return type;
    }

    protected void setType(String type) {
        this.type = Optional.of(type);
        this.typeProperty.set(type);
    }

    public LiftReversal getMain() {
        return main;
    }

    protected void setMain(LiftReversal main) {
        this.main = main;
    }

    public StringProperty typeProperty() {
        return typeProperty;
    }
}
