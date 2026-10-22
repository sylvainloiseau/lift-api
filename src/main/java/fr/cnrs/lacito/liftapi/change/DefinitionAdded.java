package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/// A field or trait definition added to the header.
///
/// @param dictionary the dictionary that changed
/// @param header the header
/// @param definition the definition added
public record DefinitionAdded(LiftDictionary dictionary, LiftHeader header, LiftFieldAndTraitDefinition definition) implements ModelChange {

    @Override
    public Object subject() {
        return definition;
    }

    @Override
    public void revert() {
        header.removeFieldOrTraitDefinition(definition);
    }

    @Override
    public void reapply() {
        // The traits and fields the addition rebound are put back by their own changes.
        ModelInternals.get().withoutRebinding(header, () -> header.addFieldOrTraitDefinition(definition));
    }
}
