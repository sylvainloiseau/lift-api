package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import java.util.OptionalInt;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;

/// A component attached to a parent, or an entry added to the dictionary.
///
/// The component carries its whole subtree: building it before attaching it reports
/// nothing, and this one event stands for all of it.
///
/// @param dictionary the dictionary that changed
/// @param parent the object now holding the component: a component, a
///        {@link fr.cnrs.lacito.liftapi.model.MultiText} or a
///        {@link fr.cnrs.lacito.liftapi.model.Form} for an annotation, or the
///        {@link LiftDictionary} for an entry
/// @param component the component added
/// @param position its index in its parent's list; empty for a component held in a map
///        (a note, a field, a translation) or in a single slot (a grammatical info, the
///        main of a reversal), which a reapply puts back under its key or in its slot
/// @param registered whether this addition registered the component in the
///        dictionary's indexes; {@code false} when it was attached again after a
///        {@link AbstractLiftRoot#detach()}, or when it belongs to the header
public record ComponentAdded(
    LiftDictionary dictionary,
    Object parent,
    AbstractLiftRoot component,
    OptionalInt position,
    boolean registered
) implements ModelChange {

    @Override
    public Object subject() {
        return component;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            if (registered) {
                Slots.delete(parent, component);
            } else {
                component.detach();
            }
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            Slots.add(parent, component, position);
        });
    }
}
