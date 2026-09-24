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
 */
package fr.cnrs.lacito.liftapi.model;
