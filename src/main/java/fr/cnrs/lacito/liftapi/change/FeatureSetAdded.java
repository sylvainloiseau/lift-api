package fr.cnrs.lacito.liftapi.change;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/// A feature set (a LIFT {@code range}) added to the header.
///
/// @param dictionary the dictionary that changed
/// @param header the header
/// @param featureSet the feature set added
public record FeatureSetAdded(LiftDictionary dictionary, LiftHeader header, FeatureSet featureSet) implements ModelChange {

    @Override
    public Object subject() {
        return featureSet;
    }

    @Override
    public void revert() {
        header.removeFeatureSet(featureSet);
    }

    @Override
    public void reapply() {
        header.addFeatureSet(featureSet);
    }
}
