package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/// A feature set removed from the header.
///
/// @param dictionary the dictionary that changed
/// @param header the header
/// @param featureSet the feature set removed, with its features
public record FeatureSetRemoved(LiftDictionary dictionary, LiftHeader header, FeatureSet featureSet) implements ModelChange {

    @Override
    public Object subject() {
        return featureSet;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            // The traits the addition rebound are put back by their own changes.
            ModelInternals.get().withoutRebinding(header, () -> header.addFeatureSet(featureSet));
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutExternalDefinitionGuard(() -> {
            header.removeFeatureSet(featureSet);
        });
    }
}
