package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.LiftDictionary;

/// A value replaced: a text, a date, a type, an id, a trait value...
///
/// @param dictionary the dictionary that changed
/// @param owner the object holding the value (a component, a {@link fr.cnrs.lacito.liftapi.model.Form},
///        a {@link fr.cnrs.lacito.liftapi.model.Feature}, a field or trait definition)
/// @param property which value of {@code owner} changed
/// @param oldValue the value before the change; {@code null} when there was none
/// @param newValue the value after the change; {@code null} when there is none
public record ValueChanged<T>(
    LiftDictionary dictionary,
    Object owner,
    ModelProperty<T> property,
    T oldValue,
    T newValue
) implements ModelChange {

    @Override
    public Object subject() {
        return owner;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            property.apply(owner, oldValue);
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            property.apply(owner, newValue);
        });
    }
}
