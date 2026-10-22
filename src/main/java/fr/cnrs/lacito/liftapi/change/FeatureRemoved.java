package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;

/// A feature removed from a feature set.
///
/// @param dictionary the dictionary that changed
/// @param featureSet the feature set
/// @param feature the feature removed; a revert puts back this very object, so that
///        whatever still refers to it is valid again
public record FeatureRemoved(LiftDictionary dictionary, FeatureSet featureSet, Feature feature) implements ModelChange {

    @Override
    public Object subject() {
        return feature;
    }

    @Override
    public void revert() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            featureSet.addFeature(feature);
        });
    }

    @Override
    public void reapply() {
        ModelInternals.get().withoutDefinitionGuard(() -> {
            featureSet.removeFeature(feature.getId());
        });
    }
}
