package fr.cnrs.lacito.liftapi;

import java.util.Objects;

/**
 * What {@link LiftDictionary#extract(java.util.Collection, ExtractOptions)} copies besides
 * the entries.
 *
 * @param header     how much of the header
 * @param references what becomes of a relation or a variant whose target is not extracted
 */
public record ExtractOptions(HeaderScope header, ReferenceTargets references) {

    /** How much of the header an extract holds. */
    public enum HeaderScope {
        /**
         * The header description, and the ranges, values and field and trait definitions
         * the entries use, a value with the values above it. A range read from an external
         * file is copied into the extract. The default: cheap whatever the size of the
         * header.
         */
        USED,
        /** The whole header, external ranges read again from their files: a complete dictionary. */
        FULL
    }

    /** What becomes of a reference to an entry or a sense outside the extract. */
    public enum ReferenceTargets {
        /** The relation or the variant keeps the LIFT id of its target, and has no target object. */
        IDS_ONLY,
        /**
         * The target's entry is added as a stub (its LIFT id, forms, and the ids and glosses
         * of its senses), so that the reference can be shown: see
         * {@link LiftDictionary#isReferenceStub}.
         */
        STUBS
    }

    /** {@link HeaderScope#USED} and {@link ReferenceTargets#IDS_ONLY}. */
    public static final ExtractOptions DEFAULT = new ExtractOptions(HeaderScope.USED, ReferenceTargets.IDS_ONLY);

    public ExtractOptions {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(references, "references");
    }

    public ExtractOptions withHeader(HeaderScope scope) {
        return new ExtractOptions(scope, references);
    }

    public ExtractOptions withReferences(ReferenceTargets targets) {
        return new ExtractOptions(header, targets);
    }
}
