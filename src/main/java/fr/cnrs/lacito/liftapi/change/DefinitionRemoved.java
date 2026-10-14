package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
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
        header.addFieldOrTraitDefinition(definition);
    }

    @Override
    public void reapply() {
        header.removeFieldOrTraitDefinition(definition);
    }
}
