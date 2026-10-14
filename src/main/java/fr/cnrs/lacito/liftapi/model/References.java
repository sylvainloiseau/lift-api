package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.internal.DictionaryMutator;

/**
 * Keeps the dictionary's reference counts in step when a relation or a variant changes
 * what it refers to.
 */
final class References {

    private References() {}

    /** The LIFT id {@code source} refers to, as the dictionary counts it. */
    static String target(HasRefId source) {
        return DictionaryMutator.referenceTarget(source);
    }

    /**
     * After {@code source} changed its reference: move its count from
     * {@code oldTarget} to its current target, if it is registered.
     */
    static void retarget(HasRefId source, String oldTarget) {
        AbstractLiftRoot node = (AbstractLiftRoot) source;
        if (node.getUUID() == null) {
            return;
        }
        LiftDictionary d = node.getOwningDictionary();
        if (d != null) {
            d.getMutator().retarget(source, oldTarget, target(source));
        }
    }
}
