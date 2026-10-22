package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * The {@code String} counterpart of {@link TrackedProperty}, for the values exposed as a
 * {@code StringProperty}.
 */
final class TrackedStringProperty extends ReadOnlyStringWrapper {

    private final ModelProperty<? super String> key;
    private String last;

    TrackedStringProperty(Object owner, ModelProperty<? super String> key, String initialValue) {
        super(owner, key.name(), initialValue);
        this.key = key;
        this.last = initialValue;
    }

    /** Refused for a value of a feature set defined in an external file (see {@link ExternalDefinitionGuard}). */
    @Override
    public void set(String newValue) {
        ExternalDefinitionGuard.requireWritable(getBean());
        super.set(newValue);
    }

    @Override
    protected void invalidated() {
        String now = get();
        String old = last;
        last = now;
        Changes.valueChangedUnchecked(getBean(), key, old, now);
    }
}
