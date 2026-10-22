/**
 * Ojects for representing exhaustively the LIFT dictionary data model
 * as defined in <a href="https://github.com/sillsdev/lift-standard">https://github.com/sillsdev/lift-standard</a>
 * ({@link LiftEntry}, {@link LiftSense}, {@link LiftExample}, etc)
 * as well as interfaces for shared behaviors (Has* : {@link HasTrait}, {@link HasField}, {@link HasType}).
 *
 * <h2>Attached and detached components</h2>
 *
 * A component is <strong>attached</strong> when walking its parent chain
 * ({@link AbstractLiftRoot#getParentNode()}) reaches a {@link LiftEntry} that belongs to
 * a dictionary; otherwise it is <strong>detached</strong>.
 * {@link AbstractLiftRoot#getOwningDictionary()} answers the question directly, and
 * returns {@code null} for a detached component.
 *
 * Being attached is what makes a component real to the dictionary: it has a UUID, it is
 * in the registries behind {@code LiftDictionaryRegistry.getSenses()} and friends, it
 * shows up in the observable lists the editing UI binds to, and the languages of its
 * {@link MultiText}s are counted towards the dictionary's language managers. A detached
 * component has none of that. It is a perfectly valid object - you can build one, read
 * it, mutate it - it simply is not part of any dictionary yet.
 *
 * <h2>The one rule</h2>
 *
 * <blockquote>
 * {@code addX()} registers its argument, and {@code deleteX()} unregisters it, if and
 * only if the receiver is attached.
 * </blockquote>
 *
 * So {@code sense.addExample(example)} on a sense that belongs to a dictionary registers
 * the example and everything under it, in that dictionary, and
 * {@code sense.deleteExample(example)} takes the whole subtree back out. The same calls
 * on a detached sense only wire and unwire. Nothing else needs to be called, and no
 * component can end up sitting in the tree without the dictionary knowing about it - or,
 * in the mirror case, left in the dictionary's indexes after being cut out of the tree.
 *
 * These two are the whole mutation API for components. A lexical entry is the one
 * exception, because it has no parent to add it to or delete it from:
 * {@code LiftDictionary.addEntry(entry)} and {@code LiftDictionary.removeEntry(entry)}
 * do that job. Everything that actually writes the dictionary's indexes lives in
 * {@code fr.cnrs.lacito.liftapi.internal}, a package this module does not export, so it
 * cannot be reached - or got wrong - from outside the library.
 *
 * Two consequences follow, and both are deliberate:
 *
 * <ul>
 * <li>The public constructors and {@code create()} factories produce <em>detached</em>
 * components. They are inert: mutating them touches no registry, and they contribute
 * nothing to any query until they are attached. Building a subtree outside a dictionary
 * is a supported thing to do - it is what the XML reader does for every
 * {@code <entry>}, and what a cut-and-paste or an undo buffer needs.</li>
 * <li>The fluent API ({@code LiftDictionary.getComponentBuilder()}) is still the
 * convenient way to <em>create</em> a component in a dictionary - it validates
 * arguments and fills in defaults - but it is no longer the only way to keep the
 * dictionary consistent.</li>
 * </ul>
 *
 * <h2>Detaching versus removing</h2>
 *
 * The two are different operations and the difference matters:
 *
 * <ul>
 * <li>{@link AbstractLiftRoot#detach()} unlinks a component from its parent and
 * <em>keeps it registered</em>. Use it to move a subtree within the same dictionary;
 * attaching it again somewhere else costs nothing, because adoption is idempotent for
 * components the dictionary already holds. A subtree left in that state must be
 * re-attached or removed as a whole - editing it in place is refused, because a
 * component deleted from it could not be taken out of the indexes it is still in.</li>
 * <li>{@code parent.deleteX(child)} unlinks <em>and</em> unregisters. The subtree comes
 * back UUID-free and can be attached anywhere, including in a different dictionary.</li>
 * </ul>
 *
 * Attaching a component that is still registered in <em>another</em> dictionary is
 * refused, rather than silently accepted: delete it from its current parent first.
 *
 * <h2>What the name of a trait or a field refers to</h2>
 *
 * A trait's name refers to a definition of the header, to a range (its id), or to
 * nothing - LIFT's "informal" name: {@link TraitSpecification}, whose cases are
 * {@link LiftFieldAndTraitDefinition}, {@link RangeReference} and {@link UndeclaredName}.
 * A field's name refers to a definition or to nothing ({@link FieldSpecification}). Only
 * definitions are written in the header. A trait's or field's specification is always
 * what its name resolves to in the header as it is
 * ({@link LiftHeader#resolveTraitSpecification(String)}): adding a definition or a range,
 * or renaming a range, rebinds the traits and fields of that name, so that the model is
 * what a save and a reload give. See {@link LiftHeader}.
 *
 * <h2>Ranges defined in an external file</h2>
 *
 * A header {@code <range href="...">} takes its content from a {@code .lift-ranges} file,
 * which the reader reads and the writer never writes: such a feature set
 * ({@link FeatureSet#isExternallyDefined()}) is read-only - its id, texts, features, their
 * hierarchy and texts, and the traits, fields and annotations they hold - until
 * {@link LiftHeader#makeFeatureSetInternal(FeatureSet)}. Every mutator of what a feature
 * set holds checks it; so do the tracked properties, bindings included. The
 * {@code <range>} of the {@code .lift} file may override the external file; what it
 * declares is flagged ({@link FeatureSet#isDeclaredInline(Feature)}) and written back.
 *
 * <h2>Declared or not</h2>
 *
 * A value its range does not declare - a trait value, a type of note or relation, a part
 * of speech - is a bare feature the reader and the builders create
 * ({@link Feature#isDeclared()} is false); a built-in feature set the file does not declare
 * is not declared either ({@link FeatureSet#isDeclared()}). Neither is written, so that a
 * file loaded and saved is the file read, and both are read-only until
 * {@link LiftHeader#declareFeature(Feature)} or {@link LiftHeader#declareFeatureSet(FeatureSet)}.
 *
 * <h2>Naming</h2>
 *
 * <ul>
 * <li>{@code addX} / {@code deleteX} for components: they register and unregister (see
 * above).</li>
 * <li>{@link AbstractLiftRoot#detach()} to move a component: it unlinks and keeps the
 * registration.</li>
 * <li>{@code removeX} for what is not a component: forms
 * ({@link MultiText#removeForm}), features, feature sets, definitions, languages. Like
 * the deletion of a referenced entry, the removal of a feature, a feature set or a
 * definition in use is refused ({@link LiftHeader#isUsed(Feature)},
 * {@link LiftHeader#isUsed(FeatureSet)}).</li>
 * <li>{@code setX} for a value; {@code retypeX} on a parent, which {@code setType} calls;
 * {@link FeatureSet#changeFeatureId} and {@link LiftHeader#renameFeatureSet} for the ids
 * of the header, which re-key what holds them.</li>
 * </ul>
 *
 * <h2>One way to change each thing</h2>
 *
 * The collections a getter returns ({@code getSenses()}, {@code getNotes()},
 * {@code getFields()}...) are read-only observable views: a UI binds to them, and changes
 * them with {@code addX}/{@code deleteX}, which keep the registry in step. Values are
 * JavaFX properties: writable ones ({@code dateModifiedProperty()},
 * {@code Form.textProperty()}...) when setting them has no other consequence, read-only
 * ones with a setter that does the rest otherwise ({@code idProperty()} and
 * {@code setId}, {@code typeProperty()} and {@code setType}). Every change made through
 * either is reported to the dictionary's listeners (see the {@code change} package).
 *
 * Most of these properties are created on the first call to their accessor: until then
 * the value lives in a plain field. A dictionary holds a great many components, few of
 * which are ever bound to a UI.
 *
 * <h2>Order</h2>
 *
 * The components held in a list - senses, examples, traits, annotations, relations,
 * variants, pronunciations, etymologies, reversals, media, illustrations - are in a
 * meaningful order, and every {@code addX} has an {@code addX(int index, X)} counterpart;
 * {@link AbstractLiftRoot#positionInParent()} gives the position of one. Those held in a
 * map - notes, fields, translations, the forms of a multitext - are keyed by type, name or
 * language, and have no order: the writer sorts them, so that a file saved twice is the
 * same.
 */
package fr.cnrs.lacito.liftapi.model;
