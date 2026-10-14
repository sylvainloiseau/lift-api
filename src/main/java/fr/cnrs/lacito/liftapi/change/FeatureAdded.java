package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;

/// A feature (a LIFT {@code range-element}) added to a feature set.
///
/// @param dictionary the dictionary that changed
/// @param featureSet the feature set (a LIFT {@code range})
/// @param feature the feature added
public record FeatureAdded(LiftDictionary dictionary, FeatureSet featureSet, Feature feature) implements ModelChange {

    @Override
    public Object subject() {
        return feature;
    }

    @Override
    public void revert() {
        featureSet.removeFeature(feature.getId());
    }

    @Override
    public void reapply() {
        featureSet.addFeature(feature);
    }
}
