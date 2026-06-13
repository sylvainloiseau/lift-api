package fr.cnrs.lacito.liftapi.model;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

// between 0.13 and 0.15 : field -> field-definition, field/@tag -> field-definition/@name,
/**
 * A field-definition (LIFT {@code <field-definition>}) describes a particular field or trait type.
 *
 * A field definition gives information about a particular field type that may be used by an
application to add information not part of the LIFT standard. The goal is that data can fully
transferred between copies of the same program that reads or writes LIFT files. A different
program may or may not be able to make full use of data provided by field (or trait)
elements. (Despite its name, a field-definition may apply to a trait as well as to a field.) (lift specification : 11)
 * 
 * The {@link #getKind()} method returns whether this definition
 * represents a {@link LiftFieldAndTraitDefinitionKind#FIELD} or a {@link LiftFieldAndTraitDefinitionKind#TRAIT}.
 * 
 * A field definition contains :
 * 
 * - name [Required, key] : This key corresponds to the name attribute found in all fields (or traits) for which this is the definition.
 * 
 * - class: [Optional, string] This attribute provides the name of the LIFT element that
contains all fields (or traits) for which this is the definition. If more than
one LIFT element may contain such fields or traits, the value of class may
be a space-separated list of element names.

- type [Optional, string] This attribute defines the basic data type of the data. 

  - For a
field-definition which describes a trait, the type attribute tells us about
the contents of the trait's value attribute. While any non-empty string may
be used for the type, standard values are:
• “datetime” (ISO 8601) (zero or one trait)
• “integer” (zero or one trait)
• “option” (zero or one trait)
• “option-collection” (unordered references) (zero or more traits)
• “option-sequence” (ordered references) (zero or more traits)

Note, for the “option” choices, the range referenced is specified by an
accompanying option-range attribute, described next.

  - For a field-definition which describes a field, the type attribute tells us
about the contents of the element. Standard values are:
• “multistring” (0 or more parallel strings in different writing systems,
each a single paragraph of text or less)
• “multitext” (0 or more parallel strings in different writing systems,
each possibly containing multiple paragraphs)
 * 
- option-range [Optional, key] This attribute is valid only for a field-definition
that contains one of the option type values. Its value must match against an id
attribute of a range element in the header.

- writing-system [Optional, string] Provides the default list of writing systems for
displaying the information in this field. These are given as a space delimited
list of language tags (RFC 5646). This list does not limit which languages or
writing systems may contain data in the field, just which ones are displayed by
default.
Note: at this time, it is not possible to declare a multiplicity other than that implied by the
type attribute. For example, it is not currently possible to declare that a text field may be
repeated.

 * When {@code @type} is {@code option}, {@code option-collection}, or {@code option-sequence},
 * the {@code @option-range} attribute may reference a {@link LiftHeaderRange} that enumerates
 * the allowed values. After the header is fully parsed, call
 * {@link #resolveRange(LiftHeader)} to link this definition to the actual range object.
 * 
 * 
 *
 * @see LiftFieldAndTraitDefinitionKind
 * @see LiftFieldAndTraitDefinitionType
 * @see LiftFieldAndTraitDefinitionTarget
 */
public final class LiftFieldAndTraitDefinition extends AbstractLiftRoot {

    @Getter final String name;
    final LiftHeader parent;

    /** Raw {@code @option-range} attribute value (range id). */
    @Getter @Setter Optional<String> optionRange = Optional.empty();
    @Getter @Setter Optional<String> writingSystem = Optional.empty();

    @Getter MultiText label = new MultiText();

    @Getter @Setter private LiftFieldAndTraitDefinitionKind kind = LiftFieldAndTraitDefinitionKind.UNKNOWN;
    @Getter @Setter private Optional<LiftFieldAndTraitDefinitionType> definitionType = Optional.empty();
    @Getter private Set<LiftFieldAndTraitDefinitionTarget> targets = EnumSet.noneOf(LiftFieldAndTraitDefinitionTarget.class);

    /** Resolved link to the LiftHeaderRange named by {@code @option-range}, set after parsing. */
    @Getter private Optional<LiftHeaderRange> resolvedRange = Optional.empty();

    public LiftFieldAndTraitDefinition(String tag, LiftHeader parent) {
        this.name = tag;
        this.parent = parent;
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    /** Raw @class value (for backward compat / serialization). */
    public Optional<String> getFClass() {
        return targets.isEmpty() ? Optional.empty() : Optional.of(LiftFieldAndTraitDefinitionTarget.toClassAttribute(targets));
    }

    /** Set from raw @class attribute string (space-separated tokens). */
    public void setFClass(Optional<String> fClass) {
        this.targets = fClass.map(LiftFieldAndTraitDefinitionTarget::parseClassAttribute)
            .orElse(EnumSet.noneOf(LiftFieldAndTraitDefinitionTarget.class));
    }

    // public void setTargets(Set<LiftFieldAndTraitDefinitionTarget> targets) {
    //     this.targets = targets != null ? targets : EnumSet.noneOf(LiftFieldAndTraitDefinitionTarget.class);
    // }

    /** Raw @type value (for serialization). */
    public Optional<String> getType() {
        return definitionType.map(LiftFieldAndTraitDefinitionType::toLiftValue);
    }

    /** Set from raw @type attribute string, resolving the enum and kind. */
    public void setType(Optional<String> typeStr) {
        this.definitionType = typeStr.flatMap(LiftFieldAndTraitDefinitionType::fromLiftValue);
        this.kind = this.definitionType
            .map(LiftFieldAndTraitDefinitionKind::fromType)
            .orElse(LiftFieldAndTraitDefinitionKind.UNKNOWN);
    }

    public boolean isFieldDefinition() {
        return kind == LiftFieldAndTraitDefinitionKind.FIELD;
    }

    public boolean isTraitDefinition() {
        return kind == LiftFieldAndTraitDefinitionKind.TRAIT;
    }

    /**
     * After the header is fully parsed, resolve the {@code @option-range} string to the
     * actual {@link LiftHeaderRange} in the given header. Call this once from
     * {@link LiftFactory#resolveFieldDefinitionKinds()}.
     */
    public void resolveRange(LiftHeader header) {
        this.resolvedRange = optionRange.flatMap(rangeId ->
            header.getRanges().stream()
                .filter(r -> rangeId.equals(r.getId()))
                .findFirst());
    }
}
