package fr.cnrs.lacito.liftapi.model;

/**
 * Keeps read-only the definitions the writer does not write: the feature sets defined in an
 * external ranges file, the feature sets the file does not declare, and the features their
 * range does not declare.
 *
 * <ul>
 * <li>A feature set whose header {@code <range>} has an {@code href} takes its content from
 * a {@code .lift-ranges} file, which is not written on save: the header only writes the
 * reference (see {@link FeatureSet#isExternallyDefined()}). Read-only until
 * {@link LiftHeader#makeFeatureSetInternal(FeatureSet)}.</li>
 * <li>A built-in feature set the file does not declare ({@link FeatureSet#isDeclared()}) is
 * not written at all. Read-only until {@link LiftHeader#declareFeatureSet(FeatureSet)}.</li>
 * <li>A feature its range does not declare ({@link Feature#isDeclared()}) - a bare one
 * created for a value a component uses - is not written. Read-only until
 * {@link LiftHeader#declareFeature(Feature)}. The other features of its set are not
 * concerned.</li>
 * </ul>
 *
 * A change to any of them would be lost on save, and memory would no longer be what a
 * reload gives. So it is refused.
 *
 * Every mutator of what a feature set holds calls {@link #requireWritable(Object)} before
 * changing anything; the tracked properties do it in their {@code set}, so a binding is
 * checked too. The check walks up from the changed object: the first feature met must be
 * declared, and the feature set must be declared and not external. Whether a feature or a
 * set is declared, and the {@code href}, are not guarded: they are how a definition is made
 * writable.
 *
 * The guard is suspended (per thread: a dictionary is confined to one thread, but two
 * dictionaries may be loaded on two threads) while:
 * <ul>
 * <li>the reader fills a set (the {@code href} is only set at the end of its
 * {@code <range>}, but the {@code parent} of range elements is resolved at the end of the
 * header);</li>
 * <li>the reader or a builder creates a bare feature a component uses and its range does
 * not declare (a note type, a relation type...): the reload creates it the same way, so
 * this is no change of the definition;</li>
 * <li>traits are rebound to what their name resolves to: their XML does not change;</li>
 * <li>changes are reverted or reapplied: they put back a state that was valid.</li>
 * </ul>
 */
final class DefinitionGuard {

    private static final ThreadLocal<int[]> SUSPENSIONS = ThreadLocal.withInitial(() -> new int[1]);

    private DefinitionGuard() {}

    /**
     * Refuses a change of {@code changed} if it belongs to a feature its range does not
     * declare, or to a feature set that is external or not declared.
     *
     * @param changed a component, a MultiText or a form about to change
     * @throws IllegalStateException if the definition it belongs to is read-only
     */
    static void requireWritable(Object changed) {
        if (SUSPENSIONS.get()[0] > 0) {
            return;
        }
        Object o = changed;
        boolean featureChecked = false;
        while (o != null) {
            switch (o) {
                case FeatureSet set -> {
                    requireWritable(set);
                    return;
                }
                case Feature f -> {
                    if (!featureChecked && !f.isDeclared()) {
                        throw new IllegalStateException(
                            "The value " + f.getId() + " of the range " + f.getParentFeatureSet().getId()
                                + " is not declared; declare it to change it.");
                    }
                    featureChecked = true;
                    o = f.getParentFeatureSet();
                }
                case LiftAnnotation a -> o = a.getParent();
                case LiftHeader h -> o = null;
                case AbstractLiftRoot r -> o = r.getParentNode();
                case MultiText m -> o = m.getParent();
                case Form f -> o = f.getMultiText();
                default -> o = null;
            }
        }
    }

    private static void requireWritable(FeatureSet set) {
        if (set.isExternallyDefined()) {
            throw new IllegalStateException(
                "The range " + set.getId() + " is defined in the external file " + set.getHref().orElse("")
                    + " and cannot be changed; make it internal to change it.");
        }
        if (!set.isDeclared()) {
            throw new IllegalStateException(
                "The range " + set.getId() + " is not declared in the file; declare it to change it.");
        }
    }

    /** Runs {@code operation} without the guard. */
    static void withoutGuard(Runnable operation) {
        int[] suspensions = SUSPENSIONS.get();
        suspensions[0]++;
        try {
            operation.run();
        } finally {
            suspensions[0]--;
        }
    }
}
