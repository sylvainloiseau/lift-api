package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * An object property that reports its changes as {@link ModelProperty} value changes.
 *
 * It overrides {@link #invalidated()} rather than registering a listener, so it costs
 * nothing more than a plain property, and whatever sets it - a setter, a binding, a
 * bidirectional binding with a UI control - is reported the same way.
 *
 * It is a {@link ReadOnlyObjectWrapper}: a component hands out either the property itself
 * (a writable value) or its {@link #getReadOnlyProperty()} (a value changed through a
 * setter that keeps something else in step).
 */
final class TrackedProperty<T> extends ReadOnlyObjectWrapper<T> {

    private final ModelProperty<? super T> key;
    private T last;

    TrackedProperty(Object owner, ModelProperty<? super T> key, T initialValue) {
        super(owner, key.name(), initialValue);
        this.key = key;
        this.last = initialValue;
    }

    /**
     * Refused for a value of a feature set defined in an external file (see
     * {@link ExternalDefinitionGuard}); the {@code href} itself is how such a set is made
     * internal, and is not guarded.
     */
    @Override
    public void set(T newValue) {
        if (key != ModelProperty.HREF) {
            ExternalDefinitionGuard.requireWritable(getBean());
        }
        super.set(newValue);
    }

    @Override
    protected void invalidated() {
        // get() also revalidates the property, so that the next set() is seen too.
        T now = get();
        T old = last;
        last = now;
        Changes.valueChangedUnchecked(getBean(), key, old, now);
    }
}
