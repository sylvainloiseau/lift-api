package fr.cnrs.lacito.liftapi;

/**
 * The version of the LIFT specification a dictionary conforms to.
 *
 * This is a property of the dictionary itself (see
 * {@link LiftDictionary#getLiftVersion()}), not of its XML serialization: it decides
 * which elements and attributes the model may carry, and the reader and writer follow
 * it. It therefore lives here rather than in the (module-internal) {@code xml} package.
 */
public enum LiftVersion {
    V0_13,
    V0_15;
}
