package fr.cnrs.lacito.liftapi.model;

import java.util.*;

/**
 * Represents the LIFT element types that a {@code <field-definition>} may apply to,
 * as declared in the {@code @class} attribute (space-separated token list).
 * <p>
 * Example: {@code class="entry sense"} means the field/trait can appear on entries and senses.
 *
 * @see <a href="https://github.com/sillsdev/lift-standard/blob/master/lift_15.pdf">LIFT spec p.11</a>
 */
public enum FieldDefinitionTarget {

    ENTRY("entry"),
    SENSE("sense"),
    EXAMPLE("example"),
    VARIANT("variant"),
    PRONUNCIATION("pronunciation"),
    NOTE("note"),
    ETYMOLOGY("etymology"),
    RELATION("relation"),
    REVERSAL("reversal"),
    RANGE("range"),
    RANGE_ELEMENT("range-element");

    private final String liftValue;

    FieldDefinitionTarget(String liftValue) {
        this.liftValue = liftValue;
    }

    public String toLiftValue() {
        return liftValue;
    }

    public static Optional<FieldDefinitionTarget> fromLiftValue(String token) {
        if (token == null) return Optional.empty();
        String t = token.trim().toLowerCase();
        for (FieldDefinitionTarget v : values()) {
            if (v.liftValue.equals(t)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Parse a space-separated {@code @class} attribute value into a set of targets.
     * Unknown tokens are silently ignored.
     */
    public static Set<FieldDefinitionTarget> parseClassAttribute(String classAttr) {
        if (classAttr == null || classAttr.isBlank()) return EnumSet.noneOf(FieldDefinitionTarget.class);
        Set<FieldDefinitionTarget> result = EnumSet.noneOf(FieldDefinitionTarget.class);
        for (String token : classAttr.trim().split("\\s+")) {
            fromLiftValue(token).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Serialize a set of targets back to a space-separated string for the {@code @class} attribute.
     */
    public static String toClassAttribute(Set<FieldDefinitionTarget> targets) {
        if (targets == null || targets.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(" ");
        for (FieldDefinitionTarget t : targets) sj.add(t.liftValue);
        return sj.toString();
    }
}
