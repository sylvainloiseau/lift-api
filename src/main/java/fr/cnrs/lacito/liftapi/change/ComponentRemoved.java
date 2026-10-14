package fr.cnrs.lacito.liftapi.change;

import java.util.OptionalInt;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;

/// A component removed from its parent, with its whole subtree, or an entry removed
/// from the dictionary.
///
/// @param dictionary the dictionary that changed
/// @param parent the object that held the component (see {@link ComponentAdded#parent()})
/// @param component the component removed; it keeps its subtree, so a revert puts back
///        the same objects
/// @param position its index in its parent's list before the removal; empty for a
///        component held in a map or in a single slot
/// @param unregistered {@code true} for a {@code deleteX} (or
///        {@link LiftDictionary#removeEntry}), which also took the subtree out of the
///        dictionary's indexes; {@code false} for {@link AbstractLiftRoot#detach()},
///        which keeps it registered so that it can be attached again elsewhere
public record ComponentRemoved(
    LiftDictionary dictionary,
    Object parent,
    AbstractLiftRoot component,
    OptionalInt position,
    boolean unregistered
) implements ModelChange {

    @Override
    public Object subject() {
        return component;
    }

    @Override
    public void revert() {
        Slots.add(parent, component, position);
    }

    @Override
    public void reapply() {
        if (unregistered) {
            Slots.delete(parent, component);
        } else {
            component.detach();
        }
    }
}
