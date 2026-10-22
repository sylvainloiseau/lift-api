package fr.cnrs.lacito.liftapi.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The specification of a trait whose name is the id of a {@link FeatureSet} of the header,
 * with no definition of that name: the value of the trait is a feature of that set.
 *
 * The name is computed from the set, so it follows
 * {@link LiftHeader#renameFeatureSet(FeatureSet, String)}: the traits are written under
 * the new id and read back to the same set.
 *
 * Two references are equal when they refer to the same set object ({@link FeatureSet}
 * compares by identity).
 *
 * @param range the feature set the values come from
 * @see TraitSpecification
 */
public record RangeReference(FeatureSet range) implements TraitSpecification {

    public RangeReference {
        Objects.requireNonNull(range, "range");
    }

    @Override
    public String getName() {
        return range.getId();
    }

    @Override
    public LiftFieldAndTraitDefinitionDataModel getDataModel() {
        return LiftFieldAndTraitDefinitionDataModel.FEATURE;
    }

    @Override
    public Optional<FeatureSet> getResolvedFeatureSet() {
        return Optional.of(range);
    }
}
