package fr.cnrs.lacito.liftapi.model;

/**
 * Whether a {@code <field-definition>} actually defines a <b>field</b> or a <b>trait</b>.
 * <p>
 * Per the LIFT specification (p.11): <i>"Despite its name, a field-definition may apply to
 * a trait as well as to a field."</i>
 * <p>
 * The distinction is inferred from {@code @type}:
 * <ul>
 *   <li>{@code multistring} or {@code multitext} → {@link #FIELD}</li>
 *   <li>{@code datetime}, {@code integer}, {@code option}, {@code option-collection},
 *       {@code option-sequence} → {@link #TRAIT}</li>
 *   <li>absent or unrecognized → {@link #UNKNOWN} (resolved later by scanning dictionary data)</li>
 * </ul>
 */
public enum FieldDefinitionKind {
    FIELD,
    TRAIT,
    UNKNOWN;

    public static FieldDefinitionKind fromType(FieldDefinitionType type) {
        if (type == null) return UNKNOWN;
        return type.describesField() ? FIELD : TRAIT;
    }
}
