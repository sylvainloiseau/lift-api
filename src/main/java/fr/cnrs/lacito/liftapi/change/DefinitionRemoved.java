package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/// A field or trait definition removed from the header.
///
/// @param dictionary the dictionary that changed
/// @param header the header
/// @param definition the definition removed
public record DefinitionRemoved(LiftDictionary dictionary, LiftHeader header, LiftFieldAndTraitDefinition definition) implements ModelChange {

    @Override
    public Object subject() {
        return definition;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            // The traits and fields the addition rebound are put back by their own changes.
            ModelInternals.get().withoutRebinding(header, () -> header.addFieldOrTraitDefinition(definition));
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            header.removeFieldOrTraitDefinition(definition);
        });
    }
}
