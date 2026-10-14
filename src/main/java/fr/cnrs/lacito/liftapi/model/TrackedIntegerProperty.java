package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * The {@code int} counterpart of {@link TrackedProperty}, for the integer trait values.
 */
final class TrackedIntegerProperty extends SimpleIntegerProperty {

    private final ModelProperty<? super Integer> key;
    private int last;

    TrackedIntegerProperty(Object owner, ModelProperty<? super Integer> key, int initialValue) {
        super(owner, key.name(), initialValue);
        this.key = key;
        this.last = initialValue;
    }

    @Override
    protected void invalidated() {
        int now = get();
        int old = last;
        last = now;
        Changes.valueChangedUnchecked(getBean(), key, old, now);
    }
}
