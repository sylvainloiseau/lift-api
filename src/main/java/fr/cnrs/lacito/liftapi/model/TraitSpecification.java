package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * What the name of a {@link LiftTrait} refers to.
 *
 * LIFT says of a trait: <i>"Where no range is linked the name is informal or resolved by
 * its use in a field-definition"</i> (p. 13). The name of a trait therefore refers to one
 * of three things, and the model says which:
 *
 * <ul>
 * <li>a {@link LiftFieldAndTraitDefinition} declared in the header: its data model (and
 *     its {@code option-range}) say what the values are;</li>
 * <li>a {@link RangeReference}: the name is the id of a {@link FeatureSet} of the header,
 *     and the value is one of its features;</li>
 * <li>an {@link UndeclaredName}: the name is declared nowhere, and the value is a
 *     string.</li>
 * </ul>
 *
 * The specification of a trait is always what
 * {@link LiftHeader#resolveTraitSpecification(String)} returns for its name, given the
 * header as it is now: the header's operations rebind the traits they affect, or are
 * refused (see {@link LiftHeader}). So the model in memory is what a save and a reload
 * would give: the writer writes the names, the reader resolves them.
 *
 * Only declarations are written in the header: a range reference and an undeclared name
 * occupy no place among the definitions, so declaring the name later does not collide
 * with them.
 */
public sealed interface TraitSpecification
    permits LiftFieldAndTraitDefinition, RangeReference, UndeclaredName {

    /**
     * The name written on the traits: the name of the definition, the id of the range,
     * or the undeclared name.
     */
    String getName();

    /**
     * The data model of the values: the one of the definition,
     * {@link LiftFieldAndTraitDefinitionDataModel#FEATURE} for a range,
     * {@link LiftFieldAndTraitDefinitionDataModel#STRING} for an undeclared name.
     */
    LiftFieldAndTraitDefinitionDataModel getDataModel();

    /**
     * The feature set the values come from: the {@code option-range} of the definition,
     * the range itself, or empty for an undeclared name.
     */
    Optional<FeatureSet> getResolvedFeatureSet();
}
