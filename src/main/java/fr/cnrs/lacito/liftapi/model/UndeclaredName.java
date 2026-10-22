package fr.cnrs.lacito.liftapi.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The specification of a field or a trait whose name is declared nowhere: neither a
 * definition of the header nor, for a trait, the id of a range. LIFT calls such a name
 * <i>informal</i>. The value of such a trait is a string.
 *
 * Nothing is written in the header for it. Two undeclared names are equal when their
 * names are.
 *
 * @param name the name of the fields or traits
 * @see TraitSpecification
 * @see FieldSpecification
 */
public record UndeclaredName(String name) implements TraitSpecification, FieldSpecification {

    public UndeclaredName {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LiftFieldAndTraitDefinitionDataModel getDataModel() {
        return LiftFieldAndTraitDefinitionDataModel.STRING;
    }

    @Override
    public Optional<FeatureSet> getResolvedFeatureSet() {
        return Optional.empty();
    }
}
