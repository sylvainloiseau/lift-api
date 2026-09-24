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
 * {@link fr.cnrs.lacito.liftapi.model} and on the registries. That is a design choice,
 * not an accident of implementation: it is how a user interface binds to a dictionary
 * and follows edits without polling and without a hand-written observer layer, and it
 * is why {@code module-info.java} declares {@code requires transitive javafx.base}.
 *
 * Only {@code javafx.base} is required. There is no dependency on the JavaFX graphics
 * or controls modules, no toolkit to start, and no native library to load, so the
 * library works headless: the test suite exercises properties, bindings and collection
 * listeners with no JavaFX runtime initialized.
 *
 * <h2>Threading</h2>
 *
 * A {@link fr.cnrs.lacito.liftapi.LiftDictionary} and everything reachable from it is
 * <b>not safe for concurrent use</b>, and <b>must be confined to a single thread</b>.
 *
 * <h3>Why: no synchronization</h3>
 *
 * The reason is simply that the library synchronizes nothing and shares mutable state
 * widely. Adding an entry mutates a dozen registry maps, the per-language occurrence
 * counters and the UUID pool; several accessors that look like reads lazily create and
 * cache state, or register listeners, on first call. Concurrent access corrupts those
 * structures.
 *
 * This has nothing to do with JavaFX. The observable collections and properties are no
 * less thread-safe than the plain {@code HashMap} and {@code ArrayDeque} fields beside
 * them; replacing every property with an ordinary field would leave the model exactly
 * as unsafe. Note in particular that <em>reading</em> is not safe either, precisely
 * because of that lazy initialization - so "concurrent reads, single-threaded writes"
 * is not a contract this library currently offers.
 *
 * <h3>A separate constraint: FX thread affinity</h3>
 *
 * Independently of the above, once a live scene graph observes the model, the owning
 * thread must be the JavaFX Application Thread. Change listeners fire
 * <em>synchronously on the mutating thread</em>, so a background mutation runs straight
 * into scene-graph code off the FX thread, which JavaFX forbids.
 *
 * This is a thread <em>affinity</em> requirement, not a thread-safety one, and it
 * applies only while a UI is attached. Headless users - the command line tool, batch
 * processing, the tests - are free to own a dictionary on any single thread of their
 * choosing.
 *
 * <h3>In practice</h3>
 *
 * <ul>
 * <li>One thread owns a dictionary for its whole lifetime.</li>
 * <li>Parsing a large dictionary off the FX thread is fine, provided the finished
 *     instance is safely published to the FX thread before any UI binds to it and the
 *     loading thread never touches it again.</li>
 * <li>Saving reads the entire graph, so it must not run concurrently with edits: the
 *     output would be a torn mixture of states. Save from the owning thread. For scale,
 *     a 1900-entry dictionary loads in roughly 200 ms and saves in under 50 ms.</li>
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
