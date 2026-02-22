package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * Maps the {@code @type} attribute of a {@code <field-definition>} element.
 * <p>
 * For field-definitions describing a <b>trait</b>, standard values are:
 * {@link #DATETIME}, {@link #INTEGER}, {@link #OPTION},
 * {@link #OPTION_COLLECTION}, {@link #OPTION_SEQUENCE}.
 * <p>
 * For field-definitions describing a <b>field</b>, standard values are:
 * {@link #MULTISTRING}, {@link #MULTITEXT}.
 *
 * @see <a href="https://github.com/sillsdev/lift-standard/blob/master/lift_15.pdf">LIFT spec p.11</a>
 */
public enum FieldDefinitionType {

    DATETIME("datetime"),
    INTEGER("integer"),
    OPTION("option"),
    OPTION_COLLECTION("option-collection"),
    OPTION_SEQUENCE("option-sequence"),
    MULTISTRING("multistring"),
    MULTITEXT("multitext");

    private final String liftValue;

    FieldDefinitionType(String liftValue) {
        this.liftValue = liftValue;
    }

    public String toLiftValue() {
        return liftValue;
    }

    public boolean describesField() {
        return this == MULTISTRING || this == MULTITEXT;
    }

    public boolean describesTrait() {
        return !describesField();
    }

    public static Optional<FieldDefinitionType> fromLiftValue(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        for (FieldDefinitionType t : values()) {
            if (t.liftValue.equals(value.trim().toLowerCase())) return Optional.of(t);
        }
        return Optional.empty();
    }
}
