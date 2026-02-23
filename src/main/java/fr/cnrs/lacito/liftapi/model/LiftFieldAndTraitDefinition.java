package fr.cnrs.lacito.liftapi.model;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A field-definition (LIFT {@code <field-definition>}) describes a particular field or trait type.
 * <p>
 * Despite its name, a field-definition may apply to a trait as well as to a field
 * (LIFT spec p.11). The {@link #getKind()} method returns whether this definition
 * represents a {@link FieldDefinitionKind#FIELD} or a {@link FieldDefinitionKind#TRAIT}.
 * <p>
 * When {@code @type} is {@code option}, {@code option-collection}, or {@code option-sequence},
 * the {@code @option-range} attribute may reference a {@link LiftHeaderRange} that enumerates
 * the allowed values. After the header is fully parsed, call
 * {@link #resolveRange(LiftHeader)} to link this definition to the actual range object.
 *
 * @see FieldDefinitionKind
 * @see FieldDefinitionType
 * @see FieldDefinitionTarget
 */
public final class LiftFieldAndTraitDefinition extends AbstractLiftRoot {

    @Getter final String name;
    final LiftHeader parent;

    /** Raw {@code @option-range} attribute value (range id). */
    @Getter @Setter Optional<String> optionRange = Optional.empty();
    @Getter @Setter Optional<String> writingSystem = Optional.empty();

    @Getter MultiText label = new MultiText();

    @Getter @Setter private FieldDefinitionKind kind = FieldDefinitionKind.UNKNOWN;
    @Getter @Setter private Optional<FieldDefinitionType> definitionType = Optional.empty();
    @Getter private Set<FieldDefinitionTarget> targets = EnumSet.noneOf(FieldDefinitionTarget.class);

    /** Resolved link to the LiftHeaderRange named by {@code @option-range}, set after parsing. */
    @Getter private Optional<LiftHeaderRange> resolvedRange = Optional.empty();

    protected LiftFieldAndTraitDefinition(String tag, LiftHeader parent) {
        this.name = tag;
        this.parent = parent;
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    /** Raw @class value (for backward compat / serialization). */
    public Optional<String> getFClass() {
        return targets.isEmpty() ? Optional.empty() : Optional.of(FieldDefinitionTarget.toClassAttribute(targets));
    }

    /** Set from raw @class attribute string (space-separated tokens). */
    public void setFClass(Optional<String> fClass) {
        this.targets = fClass.map(FieldDefinitionTarget::parseClassAttribute)
            .orElse(EnumSet.noneOf(FieldDefinitionTarget.class));
    }

    public void setTargets(Set<FieldDefinitionTarget> targets) {
        this.targets = targets != null ? targets : EnumSet.noneOf(FieldDefinitionTarget.class);
    }

    /** Raw @type value (for serialization). */
    public Optional<String> getType() {
        return definitionType.map(FieldDefinitionType::toLiftValue);
    }

    /** Set from raw @type attribute string, resolving the enum and kind. */
    public void setType(Optional<String> typeStr) {
        this.definitionType = typeStr.flatMap(FieldDefinitionType::fromLiftValue);
        this.kind = this.definitionType
            .map(FieldDefinitionKind::fromType)
            .orElse(FieldDefinitionKind.UNKNOWN);
    }

    public boolean isFieldDefinition() {
        return kind == FieldDefinitionKind.FIELD;
    }

    public boolean isTraitDefinition() {
        return kind == FieldDefinitionKind.TRAIT;
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
