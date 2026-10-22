package fr.cnrs.lacito.liftapi.model;

/**
 * What the name of a {@link LiftField} refers to: a {@link LiftFieldAndTraitDefinition}
 * declared in the header, or an {@link UndeclaredName}.
 *
 * Unlike a trait's name (see {@link TraitSpecification}), a field's name never refers to
 * a range: the value of a field is a text.
 *
 * The specification of a field is always what
 * {@link LiftHeader#resolveFieldSpecification(String)} returns for its name, given the
 * header as it is now.
 */
public sealed interface FieldSpecification permits LiftFieldAndTraitDefinition, UndeclaredName {

    /** The name written on the fields. */
    String getName();
}
