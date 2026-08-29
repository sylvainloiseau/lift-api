package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 *
 * The various data type available on a {@link LiftTrait} or a {@link LiftField} element.
 * The data type is one of the feature of a {@link LiftFieldAndTraitDefinition}.
 * 
 * On a LiftField element, the possible values are: 
 *
 * <ul>
 *   <li>{@link #MULTISTRING}</li>
 *   <li>{@link #MULTITEXT}</li>
 * </ul>
 * 
 * On a LiftField element, the possible values are: 
 *
 * <ul>
 *   <li>{@link #DATETIME}</li>
 *   <li>{@link #INTEGER}</li>
 *   <li>{@link #FEATURE} (this correspond to the {@code OPTION} value in Lift-XML)</li>
 *   <li>{@link #FEATURE_SET} (this correspond to the {@code OPTION-COLLECTION} value in Lift-XML)</li>
 *   <li>{@link #FEATURE_LIST} (this correspond to the {@code OPTION-SEQUENCE} value in Lift-XML)</li>
 *   <li>{@link #MULTITEXT}</li>
 * </ul>
 * 
 * @see <a href="https://github.com/sillsdev/lift-standard/blob/master/lift_15.pdf">LIFT spec p.11</a>
 */
public enum LiftFieldAndTraitDefinitionDataModel {

    STRING("string"),
    DATETIME("datetime"),
    INTEGER("integer"),
    FEATURE("option"),
    FEATURE_SET("option-collection"),
    FEATURE_LIST("option-sequence"),
    MULTISTRING("multistring"),
    MULTITEXT("multitext");

    private final String str;

    LiftFieldAndTraitDefinitionDataModel(String str) {
        this.str = str;
    }

    public String toStringValue() {
        return str;
    }

    public boolean isApplicableToAField() {
        return this == MULTISTRING || this == MULTITEXT;
    }

    public boolean isApplicableToATrait() {
        return !isApplicableToAField();
    }

    public static Optional<LiftFieldAndTraitDefinitionDataModel> fromStringValue(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        for (LiftFieldAndTraitDefinitionDataModel t : values()) {
            if (t.str.toLowerCase().equals(value.trim().toLowerCase())) return Optional.of(t);
        }
        return Optional.empty();
    }
}
