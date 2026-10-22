package fr.cnrs.lacito.liftapi.model;

/**
 * Keeps the feature sets defined in an external ranges file read-only.
 *
 * A feature set whose header {@code <range>} has an {@code href} takes its content from a
 * {@code .lift-ranges} file, which is not written on save: the header only writes the
 * reference (see {@link FeatureSet#isExternallyDefined()}). A change to such a set - its
 * id, texts, features, their hierarchy, or the traits, fields and annotations they hold -
 * would therefore be lost on save, and memory would no longer be what a reload gives. So
 * it is refused, until the set is made internal
 * ({@link LiftHeader#makeFeatureSetInternal(FeatureSet)}).
 *
 * Every mutator of what a feature set holds calls {@link #requireWritable(Object)} before
 * changing anything; the tracked properties do it in their {@code set}, so a binding is
 * checked too. The check walks up from the changed object to the first feature set.
 *
 * The guard is suspended (per thread: a dictionary is confined to one thread, but two
 * dictionaries may be loaded on two threads) while:
 * <ul>
 * <li>the reader fills a set (the {@code href} is only set at the end of its
 * {@code <range>}, but the {@code parent} of range elements is resolved at the end of the
 * header);</li>
 * <li>the reader or a builder creates a bare feature a component uses and the external
 * range does not declare (a note type, a relation type...): the reload creates it the
 * same way, so this is no change of the definition;</li>
 * <li>traits are rebound to what their name resolves to: their XML does not change;</li>
 * <li>changes are reverted or reapplied: they put back a state that was valid.</li>
 * </ul>
 */
final class ExternalDefinitionGuard {

    private static final ThreadLocal<int[]> SUSPENSIONS = ThreadLocal.withInitial(() -> new int[1]);

    private ExternalDefinitionGuard() {}

    /**
     * Refuses a change of {@code changed} if it belongs to a feature set defined in an
     * external ranges file.
     *
     * @param changed a component, a MultiText or a form about to change
     * @throws IllegalStateException if it belongs to an externally defined feature set
     */
    static void requireWritable(Object changed) {
        if (SUSPENSIONS.get()[0] > 0) {
            return;
        }
        FeatureSet set = owningFeatureSet(changed);
        if (set != null && set.isExternallyDefined()) {
            throw new IllegalStateException(
                "The range " + set.getId() + " is defined in the external file " + set.getHref().orElse("")
                    + " and cannot be changed; make it internal to change it.");
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

    /** The feature set {@code o} belongs to, or {@code null}. */
    private static FeatureSet owningFeatureSet(Object o) {
        while (o != null) {
            switch (o) {
                case FeatureSet s -> {
                    return s;
                }
                case Feature f -> o = f.getParentFeatureSet();
                case LiftAnnotation a -> o = a.getParent();
                case LiftHeader h -> o = null;
                case AbstractLiftRoot r -> o = r.getParentNode();
                case MultiText m -> o = m.getParent();
                case Form f -> o = f.getMultiText();
                default -> o = null;
            }
        }
        return null;
    }
}
