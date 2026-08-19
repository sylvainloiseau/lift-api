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

    private final String str;

    LiftFieldAndTraitDefinitionTarget(String str) {
        this.str = str;
    }

    public String toStringValue() {
        return str;
    }

    public static LiftFieldAndTraitDefinitionTarget fromClassName(
        String className
    ) {
        return switch (className) {
            case "LiftEntry" -> ENTRY;
            case "LiftSense" -> SENSE;
            case "LiftExample" -> EXAMPLE;
            case "LiftVariant" -> VARIANT;
            case "LiftPronunciation" -> PRONUNCIATION;
            case "LiftNote" -> NOTE;
            case "LiftEtymology" -> ETYMOLOGY;
            case "LiftRelation" -> RELATION;
            case "LiftReversal" -> REVERSAL;
            case "LiftHeaderRange" -> RANGE;
            case "LiftHeaderRangeElement" -> RANGE_ELEMENT;
            default -> throw new IllegalArgumentException(
                "Unexpected class name: " + className
            );
        };
    }

    public static LiftFieldAndTraitDefinitionTarget fromType(AbstractLiftRoot o) {
        return switch (o) {
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
            default -> throw new IllegalArgumentException(
                "Unexpected class name: " + o.getClass().getSimpleName()
            );
        };
    }

    public static LiftFieldAndTraitDefinitionTarget fromString(String str) {
        return switch (str.toLowerCase()) {
            case "entry" -> ENTRY;
            case "sense" -> SENSE;
            case "example" -> EXAMPLE;
            case "variant" -> VARIANT;
            case "pronunciation" -> PRONUNCIATION;
            case "note" -> NOTE;
            case "etymology" -> ETYMOLOGY;
            case "relation" -> RELATION;
            case "reversal" -> REVERSAL;
            case "headerRange" -> RANGE;
            case "headerRangeElement" -> RANGE_ELEMENT;
            default -> throw new IllegalArgumentException(
                "Unexpected string: " + str
            );
        };
    }

    public static Optional<LiftFieldAndTraitDefinitionTarget> fromLiftValue(
        String token
    ) {
        if (token == null) return Optional.empty();
        String t = token.trim().toLowerCase();
        for (LiftFieldAndTraitDefinitionTarget v : values()) {
            if (v.str.equals(t)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Parse a space-separated ({@code @class} attribute) value into a set of targets.
     */
    public static Set<LiftFieldAndTraitDefinitionTarget> parseTargetString(
        String classAttr
    ) {
        Set<LiftFieldAndTraitDefinitionTarget> result = EnumSet.noneOf(
            LiftFieldAndTraitDefinitionTarget.class
        );

        if (classAttr == null || classAttr.isBlank()) return result;
        for (String token : classAttr.trim().split("\\s+")) {
            result.add(fromString(token));
        }
        return result;
    }

}
