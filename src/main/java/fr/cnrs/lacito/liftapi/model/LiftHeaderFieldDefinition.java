package fr.cnrs.lacito.liftapi.model;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A field-definition describes a particular field (or trait) type.
 * <p>
 * Despite its name, a field-definition may apply to a trait as well as to a field
 * (LIFT spec p.11).
 */
public final class LiftHeaderFieldDefinition extends AbstractLiftRoot {

    @Getter final String name;
    final LiftHeader parent;

    @Getter @Setter Optional<String> optionRange = Optional.empty();
    @Getter @Setter Optional<String> writingSystem = Optional.empty();

    @Getter MultiText label = new MultiText();

    @Getter @Setter private FieldDefinitionKind kind = FieldDefinitionKind.UNKNOWN;
    @Getter @Setter private Optional<FieldDefinitionType> definitionType = Optional.empty();
    @Getter private Set<FieldDefinitionTarget> targets = EnumSet.noneOf(FieldDefinitionTarget.class);

    protected LiftHeaderFieldDefinition(String tag, LiftHeader parent) {
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

    /** Raw @type value (for backward compat / serialization). */
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

    /**
     * Returns true if this definition describes a field (multistring/multitext),
     * false if it describes a trait, or the result of kind for UNKNOWN.
     */
    public boolean isFieldDefinition() {
        return kind == FieldDefinitionKind.FIELD;
    }

    public boolean isTraitDefinition() {
        return kind == FieldDefinitionKind.TRAIT;
    }
}
