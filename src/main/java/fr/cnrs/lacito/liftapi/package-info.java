/**
 * Main entry point for creating, editing and saving dictionaries: see
 * {@link fr.cnrs.lacito.liftapi.LiftDictionary}; the other classes in this package are
 * helpers managing the references to the components of a dictionary.
 *
 * <h2>The model is built on JavaFX</h2>
 *
 * This library deliberately exposes JavaFX types in its public API:
 * {@code javafx.beans.property.Property}, {@code javafx.collections.ObservableList},
 * {@code ObservableMap} and {@code ObservableSet} appear throughout
 * {@link fr.cnrs.lacito.liftapi.model} and on the registries. That is how a user
 * interface binds to a dictionary and follows edits without polling, and it is why
 * {@code module-info.java} declares {@code requires transitive javafx.base}. Only
 * {@code javafx.base} is needed: there is no dependency on the JavaFX graphics or
 * controls modules, so the library can be used headless.
 *
 * <h2>Threading</h2>
 *
 * A {@link fr.cnrs.lacito.liftapi.LiftDictionary} and everything reachable from it is
 * <b>not safe for concurrent use</b>. There is no internal synchronization anywhere in
 * the library, and several structures (the registries, the language counters, the UUID
 * pool) are mutated as a side effect of ordinary reads and writes.
 *
 * <ul>
 * <li>A dictionary instance must be confined to a single thread.</li>
 * <li>When a user interface observes the dictionary, that thread must be the JavaFX
 *     Application Thread: mutating the model fires change events synchronously on the
 *     calling thread, and JavaFX requires listeners on live scene graph nodes to run on
 *     the FX thread.</li>
 * <li>Loading a large dictionary off the FX thread is fine as long as the resulting
 *     instance is published to the FX thread before any UI binds to it, and is not
 *     touched from the loading thread afterwards.</li>
 * </ul>
 *
 * <h2>Per-dictionary helpers</h2>
 *
 * {@link fr.cnrs.lacito.liftapi.LiftDictionaryRegistry},
 * {@link fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager},
 * {@link fr.cnrs.lacito.liftapi.LiftDictionaryCounterManager} and
 * {@link fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory} are one
 * instance <em>per dictionary</em>, reachable from the dictionary; they are not
 * singletons and must not be shared between dictionaries.
 */
package fr.cnrs.lacito.liftapi;
