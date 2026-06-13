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
public enum LiftFieldAndTraitDefinitionTarget {

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

    LiftFieldAndTraitDefinitionTarget(String liftValue) {
        this.liftValue = liftValue;
    }

    public String toLiftValue() {
        return liftValue;
    }

    public static LiftFieldAndTraitDefinitionTarget fromType(AbstractLiftRoot o) {
        return switch(o) {
            case LiftEntry _ -> ENTRY;
            case LiftSense _ -> SENSE;
            case LiftExample _ -> EXAMPLE;
            case LiftVariant _ -> VARIANT;
            case LiftPronunciation _ -> PRONUNCIATION;
            case LiftNote _ -> NOTE;
            case LiftEtymology _ -> ETYMOLOGY;
            case LiftRelation _ -> RELATION;
            case LiftReversal _ -> REVERSAL;
            case LiftHeaderRange _ -> RANGE;
            case LiftHeaderRangeElement _ -> RANGE_ELEMENT;
            default -> throw new IllegalArgumentException("Don't recognize this type: " + o.getClass().getSimpleName());
        };
    }

    public static Optional<LiftFieldAndTraitDefinitionTarget> fromLiftValue(String token) {
        if (token == null) return Optional.empty();
        String t = token.trim().toLowerCase();
        for (LiftFieldAndTraitDefinitionTarget v : values()) {
            if (v.liftValue.equals(t)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Parse a space-separated {@code @class} attribute value into a set of targets.
     * Unknown tokens are silently ignored.
     */
    public static Set<LiftFieldAndTraitDefinitionTarget> parseClassAttribute(String classAttr) {
        if (classAttr == null || classAttr.isBlank()) return EnumSet.noneOf(LiftFieldAndTraitDefinitionTarget.class);
        Set<LiftFieldAndTraitDefinitionTarget> result = EnumSet.noneOf(LiftFieldAndTraitDefinitionTarget.class);
        for (String token : classAttr.trim().split("\\s+")) {
            fromLiftValue(token).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Serialize a set of targets back to a space-separated string for the {@code @class} attribute.
     */
    public static String toClassAttribute(Set<LiftFieldAndTraitDefinitionTarget> targets) {
        if (targets == null || targets.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(" ");
        for (LiftFieldAndTraitDefinitionTarget t : targets) sj.add(t.liftValue);
        return sj.toString();
    }
}
