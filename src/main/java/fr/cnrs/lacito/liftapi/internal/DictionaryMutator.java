package fr.cnrs.lacito.liftapi.internal;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementBuilder;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRefId;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.collections.ObservableMap;

/**
 * The single place where a dictionary is modified.
 *
 * This package is module-private, so it cannot be reached from outside the library. In order
 * to modify a dictionary, use the model API in {@code fr.cnrs.lacito.liftapi.model} or the
 * builder API in {@code fr.cnrs.lacito.liftapi.builder}.
 *
 * This class is the common core every path goes through. In particular
 * {@link #childrenOf(AbstractLiftRoot)} is the <em>only</em> definition of what a
 * component's children are, so the add and remove traversals cannot disagree.
 *
 * The two operations the rest of the library needs are
 * {@link #adoptSubtree(AbstractLiftRoot)}
 * and {@link #releaseSubtree(AbstractLiftRoot)},
 * and both are called from:
 *
 * <ul>
 * <li>{@code parent.addX(child)} on the model calls {@code adoptSubtree} through
 * {@code AbstractLiftRoot.adopted}, and {@code parent.deleteX(child)} calls
 * {@code releaseSubtree} through {@code AbstractLiftRoot.orphaned} - in both cases only
 * when {@code parent} actually belongs to a dictionary.</li>
 * <li>An entry has no parent, so {@code LiftDictionary.addEntry} and
 * {@code LiftDictionary.removeEntry} call them directly.</li>
 * </ul>
 *
 *
 * Please note the following terminological choices:
 *
 * <ul>
 * <li> <em>wiring</em> means creating the parent<->child references between two components</li>
 * <li> <em>registring</em> means adding the component to the dictionary's internal indexes</li>
 * <li> <em>attaching</em> means wiring+registering non recursively an element without UUID. Sub-components of the child not must already have been attached to it. It is the way the builder API works: it builds a subtree bottom first, registering each node as it goes, and then attach the top leaf to its parent in the dictionary</li>
 * <li> <em>adding</em></li>
 * <li> <em>adopting/release or orpheaning</em> means recursively registering a component already wired to its parent and that can, itself or its subcomponents, already have UUID belonging to the dictionary: if the UUID already exist, adopt is idempotent</li>
 * </ul>
 *
 * <h2>Not public API</h2>
 *
 * The package {@code fr.cnrs.lacito.liftapi.internal} is deliberately not exported by
 * {@code module-info.java}. The members here are {@code public} only because javac
 * requires it for access from the sibling packages inside this module; no consumer of
 * the library can reach them. That is what lets every mutating operation be gathered
 * here rather than spread over the exported classes.
 */
public final class DictionaryMutator {

    private final DictionaryRegisters registers;

    /**
     * The dictionary being mutated.
     *
     * Held so that registration can reach its language managers and stamp each entry
     * with its owner, which is what {@code AbstractLiftRoot.getOwningDictionary()}
     * resolves against.
     */
    private final LiftDictionary owner;

    public DictionaryMutator(LiftDictionary owner, DictionaryRegisters registers) {
        if (owner == null) {
            throw new IllegalArgumentException("owner cannot be null");
        }
        if (registers == null) {
            throw new IllegalArgumentException("registers cannot be null");
        }
        this.owner = owner;
        this.registers = registers;
    }

    // ------------------------------------------------------------------
    // Joining a dictionary
    // ------------------------------------------------------------------

    /**
     * Add a freshly created component to the dictionary: used by the builders
     * ({@see AbstractLiftElementBuilder#attach()}).
     *
     * The component cannot have an UUID.
     *
     * The operation is not recursive. Sub-components of the child has already
     * been attached to the child.
     *
     * The component is registered <em>before</em> it is wired to its parent, because
     * registration is what can fail (a duplicate LIFT id, for instance). Wiring first
     * would leave the parent holding a child the dictionary does not know about.
     *
     * The {@code addX} method that {@link #wire} calls is itself self-registering (see
     * {@code AbstractLiftRoot.adopted}), so the child is offered to the dictionary a
     * second time; adoption is idempotent for a component this dictionary already holds,
     * which is what makes the two safe to combine.
     *
     * @param child the newly created component
     * @param parent the component it belongs to; ignored for a {@link LiftEntry},
     *        which has no parent
     * @return {@code child}, for chaining
     */
    public <T extends AbstractLiftRoot> T attach(T child, Object parent) {
        register(child);
        wire(child, parent);
        return child;
    }

    /**
     * Register a subtree, and everything under it, in this dictionary.
     *
     * The parent and child references are expected to be wired already; only
     * registration happens here.
     *
     * Used by {@link AbstractLiftRoot#adopted()}.
     *
     * This is the path every {@code addX} on an attached component takes, and the one
     * the XML reader takes for a whole {@code <entry>}. For performance sake, no
     * builder is allocated during a parse. The SAX reader assembles an
     * entry - senses, examples, traits and all - before the entry is complete, so it
     * cannot register components one at a time as the builders do; while the entry is
     * being built it belongs to no dictionary, so none of the {@code addX} calls along
     * the way register anything, and this single traversal still does all the work.
     *
     * Adoption is idempotent for components this dictionary already holds, so a subtree
     * that was detached without being unregistered - a move within the dictionary, an
     * undo - can simply be adopted again. A component carrying a UUID this dictionary
     * does not know is refused: it belongs to another dictionary, and must be released
     * with {@link #releaseSubtree(AbstractLiftRoot)} first.
     *
     * @param root the root of the subtree to adopt
     * @throws IllegalArgumentException if {@code root} is not a {@link LiftEntry} and
     *         has no parent, or if it belongs to another dictionary
     */
    public void adoptSubtree(AbstractLiftRoot root) {
        // This is where the "a node must have a parent" invariant can finally be
        // stated: AbstractLiftRoot.getParentNode() makes the chain uniform, and by the
        // time a subtree is adopted it is already wired to its parent. Registering an
        // orphan would put a component in the indexes that no traversal can ever reach
        // again - not on delete, not on save.
        if (!(root instanceof LiftEntry) && root.getParentNode() == null) {
            throw new IllegalArgumentException(
                "Only an entry may be adopted without a parent; " +
                    root.getClass().getSimpleName() + " must be wired to its parent first."
            );
        }
        adoptRecursively(root);
    }

    /**
     * Adopt an entry and put it at a known position in the entry list.
     *
     * Entries keep the order they have in the document, so undoing the deletion of one
     * has to restore where it was, not just that it existed.
     *
     * @param entry the entry to adopt
     * @param index its position among the entries
     * @throws IllegalArgumentException if the entry is already in this dictionary, or if
     *         {@code index} is past the end of the entry list
     */
    public void adoptEntryAt(LiftEntry entry, int index) {
        if (index > registers.entries.size()) throw new IllegalArgumentException(
            "Index is greater than array size (" + index + ", " + registers.entries.size() + ")."
        );
        if (entry.getUUID() != null) throw new IllegalArgumentException(
            "This entry is already registered; its position cannot be set this way."
        );
        adoptSubtree(entry);
        // register() appended it; move it to where it belongs.
        registers.entries.removeLast();
        registers.entries.add(index, entry);
    }

    private void adoptRecursively(AbstractLiftRoot node) {
        // 1. register the node and its MultiText(s), unless we already hold it
        if (!isRegisteredHere(node)) {
            register(node);
        }
        // 2. recursively add its descendants, using the same child enumeration as
        // releaseSubtree so the two stay mirror images.
        for (AbstractLiftRoot child : childrenOf(node)) {
            adoptRecursively(child);
        }
    }

    /**
     * Whether this dictionary already holds {@code node}.
     *
     * A UUID alone does not prove it: it only says the component was registered
     * <em>somewhere</em>. The index is asked for the mapping so that a component still
     * owned by another dictionary is rejected rather than silently skipped, which would
     * leave it wired into this dictionary but absent from every lookup.
     *
     * @throws IllegalArgumentException if the node carries a UUID this dictionary does
     *         not know
     */
    private boolean isRegisteredHere(AbstractLiftRoot node) {
        UUID uuid = node.getUUID();
        if (uuid == null) {
            return false;
        }
        if (registers.nodesById(node).get(uuid) == node) {
            return true;
        }
        throw new IllegalArgumentException(
            "This node is registered in another dictionary: " +
                node.getClass().getSimpleName() + " " + uuid +
                ". Release it from that dictionary before attaching it here."
        );
    }

    /**
     * Non-recursively register one node.
     *
     * Registering a node in the dictionary means:
     *
     * <ul>
     * <li>giving it a UUID</li>
     * <li>recording the mapping (node, UUID) in the index for its kind</li>
     * <li>counting it against the components it refers to (see {@link HasRefId})</li>
     * <li>giving it a LIFT id (entries and senses only) if it does not have one</li>
     * <li>registering the MultiTexts it owns, which is what puts their languages on the
     * dictionary's language managers</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the node already has a UUID, i.e. it is
     *         already registered somewhere
     */
    private void register(AbstractLiftRoot node) {
        if (node.getUUID() != null) {
            throw new IllegalArgumentException(
                "This node seems to have already been registered in a dictionary."
            );
        }
        UUID uuid = registers.newUUID();
        node.setUUID(uuid);

        registers.nodesById(node).put(uuid, node);

        // Only entries and senses carry a LIFT id of their own; every other kind of
        // node needed nothing beyond the registration above.
        switch (node) {
            case LiftEntry e -> {
                // The one back reference from the component graph to the dictionary:
                // everything below this entry resolves getOwningDictionary() through it.
                e.setOwningDictionary(owner);
                if (e.getId().isEmpty()) {
                    e.setId(uuid.toString());
                }
                if (registers.entriesByLiftId.containsKey(e.getId().get())) {
                    throw new DuplicateIdException(
                        "Duplicate lift id: " + e.getId().get()
                    );
                }
                registers.entriesByLiftId.put(e.getId().get(), e);
                registers.entryLiftId2Uuid.put(e.getId().get(), e.getUUID());
                registers.entries.add(e);
            }
            case LiftSense s -> {
                if (s.getId().isEmpty()) {
                    s.setId(uuid.toString());
                }
                // Same guard as for entries: without it a file with two senses
                // sharing an id silently loses one of them.
                if (registers.sensesByLiftId.containsKey(s.getId().get())) {
                    throw new DuplicateIdException(
                        "Duplicate lift id: " + s.getId().get()
                    );
                }
                registers.sensesByLiftId.put(s.getId().get(), s);
                registers.senseLiftId2Uuid.put(s.getId().get(), s.getUUID());
            }
            default -> {
                // nodesById above already rejected an unknown kind of node.
            }
        }

        for (MultiText text : objectTextsOf(node)) {
            registerMultiText(
                text,
                registers.objectTextById,
                owner.getObjectLanguageManager()
            );
        }
        for (MultiText text : metaTextsOf(node)) {
            registerMultiText(
                text,
                registers.metaTextById,
                owner.getMetaLanguageManager()
            );
        }

        if (node instanceof HasRefId hasref) {
            String target = null;

            // TODO : which is available here, depending on high/low level ?
            // Try to avoid the test
            if (hasref.getRefId().isPresent() )
                target = hasref.getRefId().get();
            else if (hasref.getRefObject() != null && hasref.getRefObject().getId().isPresent())
                target = hasref.getRefObject().getId().get();

            if (target != null && !target.trim().isEmpty()) {
                registers.addReference(target, hasref);
            }
        }
    }

    /**
     * Register a MultiText and hand it the language manager it must report to.
     *
     * A MultiText created through the builders is empty at this point, so the loop
     * below does nothing and the manager keeps refusing forms in languages the
     * dictionary does not declare - which is the guard the editing UI relies on. A
     * MultiText that arrives already filled, on the other hand, comes from a subtree
     * built outside the dictionary: the XML reader assembling an entry, or a component
     * moved in from elsewhere. Its languages are part of what is being adopted, so they
     * are declared here rather than rejected. This is what replaced the parse-wide
     * "turn the language manager off and recount at the end" hack.
     */
    private void registerMultiText(MultiText element,
        ObservableMap<UUID, MultiText> textById,
        LiftDictionaryLanguagesManager languagesManager
        ) {
        if (element.getUUID() != null) {
            throw new IllegalArgumentException("UUID already set");
        }
        UUID uuid = registers.newUUID();
        element.setUUID(uuid);
        textById.put(uuid, element);
        for (String lang : element.getLangs()) {
            if (!languagesManager.hasLanguage(lang)) {
                languagesManager.addLanguage(lang);
            }
        }
        element.setLanguagesManager(languagesManager);
    }

    // ------------------------------------------------------------------
    // Leaving a dictionary
    // ------------------------------------------------------------------

    /**
     * Unregister a subtree, and everything under it, from this dictionary.
     *
     * The mirror of {@link #adoptSubtree(AbstractLiftRoot)}: the components come back
     * UUID-free, their texts stop counting towards the dictionary's languages, and the
     * subtree can then be attached anywhere, including in a different dictionary.
     *
     * Only registration is undone here. Unlinking the subtree from its parent is the
     * job of {@code parent.deleteX(child)}, which calls this first - so that a refusal
     * (a component still referred to from elsewhere) leaves the dictionary exactly as it
     * was rather than half-unlinked.
     *
     * @param root the root of the subtree to release
     * @throws IllegalStateException if the subtree is still referred to from elsewhere
     */
    public void releaseSubtree(AbstractLiftRoot root) {
        // 1. First, manage reference counting
        if (root instanceof LiftRelation r) {
            final String target = r
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            registers.removeReference(target, r);
        } else if (root instanceof LiftVariant a) {
            final String target = a
                .getRefObject().getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            registers.removeReference(target, a);

        // 2. check that this node is not refered from another node
        } else if (root instanceof AbstractIdentifiable i) {
            final String refId = i
                .getId()
                .orElseThrow(() ->
                    new IllegalArgumentException("Reference ID is missing")
                );
            if (registers.isReferenced(refId)) {
                throw new IllegalStateException(
                    "Cannot delete this node: it is referenced from other nodes."
                );
            }
        }

        // 3. remove this node from the indexes.
        // unregister() already unregisters the node's own MultiTexts; doing it again
        // here would decrement every language counter twice.
        unregister(root);

        // 4. recursively unregister its descendants, using the same child
        // enumeration as adoptSubtree so the two stay mirror images.
        for (AbstractLiftRoot child : childrenOf(root)) {
            releaseSubtree(child);
        }
    }

    /**
     * Remove one node from the indexes and clear its UUID.
     */
    private void unregister(AbstractLiftRoot node) {
        Map<UUID, ? extends AbstractLiftRoot> map = registers.nodesById(node);
        if (!map.containsKey(node.getUUID())) {
            throw new IllegalArgumentException(
                "Entry not found in registry: " + node.getUUID()
            );
        }
        map.remove(node.getUUID());

        node.setUUID(null);

        if (node instanceof AbstractIdentifiable identifiable) {
            String liftId = identifiable.getId().get();
            switch (identifiable) {
                case LiftEntry _ ->  {
                    registers.entriesByLiftId.remove(liftId);
                    registers.entryLiftId2Uuid.remove(liftId);
                }
                case LiftSense _ ->  {
                    registers.sensesByLiftId.remove(liftId);
                    registers.senseLiftId2Uuid.remove(liftId);
                }
            }
        }

        // TODO inefficient
        if (node instanceof LiftEntry e) {
            registers.entries.removeIf(x -> x == e);
            // Mirrors register(): the subtree below this entry becomes detached, so
            // mutating it no longer touches this dictionary.
            e.setOwningDictionary(null);
        }

        for (MultiText text : objectTextsOf(node)) {
            unregisterMultiText(text, registers.objectTextById);
        }
        for (MultiText text : metaTextsOf(node)) {
            unregisterMultiText(text, registers.metaTextById);
        }
    }

    private void unregisterMultiText(
        MultiText element,
        ObservableMap<UUID, MultiText> textById
    ) {
        textById.remove(element.getUUID());
        element.unregister();
        element.setUUID(null);
        element.setLanguagesManager(null);
    }

    // ------------------------------------------------------------------
    // The two traversals
    // ------------------------------------------------------------------

    /**
     * The MultiTexts of {@code node} that hold object-language material.
     *
     * Enumerating a node's texts used to be written out twice, once in the register
     * switch and once in the unregister switch, and a kind of text present in one and
     * missing from the other silently corrupted the language counters. Like
     * {@link #childrenOf(AbstractLiftRoot)}, this is now stated once.
     *
     * @param node the component whose texts are wanted
     * @return its object-language texts, possibly empty
     */
    public static List<MultiText> objectTextsOf(AbstractLiftRoot node) {
        return switch (node) {
            case LiftEntry e -> List.of(e.getMainMultiText(), e.getCitations());
            case LiftExample e -> List.of(e.getExample());
            case LiftVariant v -> List.of(v.getForms());
            case LiftReversal r -> List.of(r.getForms());
            case LiftPronunciation p -> List.of(p.getPronunciation());
            case LiftEtymology e -> List.of(e.getForms());
            case LiftSense _, LiftTrait _, GrammaticalInfo _, LiftRelation _,
                 LiftNote _, LiftMedia _, LiftIllustration _, LiftField _,
                 LiftAnnotation _ -> List.of();
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        };
    }

    /**
     * The MultiTexts of {@code node} that hold meta-language material.
     *
     * @param node the component whose texts are wanted
     * @return its meta-language texts, possibly empty
     * @see #objectTextsOf(AbstractLiftRoot)
     */
    public static List<MultiText> metaTextsOf(AbstractLiftRoot node) {
        return switch (node) {
            case LiftSense s -> List.of(s.getMainMultiText(), s.getDefinition());
            case LiftExample e -> List.copyOf(e.getTranslations().values());
            case LiftRelation r -> List.of(r.getUsage());
            case LiftNote n -> List.of(n.getText());
            case LiftMedia m -> List.of(m.getLabel());
            case LiftIllustration i -> List.of(i.getLabel());
            case LiftField f -> List.of(f.getText());
            case LiftAnnotation a -> List.of(a.getText());
            case LiftEntry _, LiftVariant _, LiftTrait _, GrammaticalInfo _,
                 LiftReversal _, LiftPronunciation _ -> List.of();
            case LiftEtymology y -> List.of(y.getGlosses());
            default -> throw new IllegalStateException(
                "Unknown type: " + node.getClass()
            );
        };
    }

    /**
     * The components owned by {@code node}, i.e. those that join and leave the
     * dictionary with it.
     *
     * This is the single definition used by both the add and the remove traversal, so
     * that the two remain mirror images by construction rather than by coincidence.
     * Note that it enumerates a node's <em>direct</em> children only; the callers
     * recurse.
     *
     * Every kind of component a node owns must appear here. Anything missing is
     * silently dropped on load and silently left behind on delete: media held by a
     * pronunciation, and the traits carried by a {@code <grammatical-info>}, were both
     * absent for exactly that reason. {@code DictionaryCensusTest} guards against the
     * next omission by comparing what a corpus contains with what the registry holds
     * after loading it.
     *
     * @param node the component whose children are wanted
     * @return the direct children, in a stable order
     */
    public static List<AbstractLiftRoot> childrenOf(AbstractLiftRoot node) {
        List<AbstractLiftRoot> children = new ArrayList<>();

        if (node instanceof LiftEntry e) {
            children.addAll(e.getVariants());
            children.addAll(e.getEtymologies());
        }

        if (node instanceof LiftSense s) {
            children.addAll(s.getExamples());
            children.addAll(s.getIllustrations());
            s.getGrammaticalInfo().ifPresent(children::add);
            // Reversals come from the HasReversal branch below, which LiftSense also
            // matches: listing them here too would visit each one twice.
        }

        if (node instanceof GrammaticalInfo gi) {
            // GrammaticalInfo is not an AbstractExtensibleWithoutField, so the branch
            // below does not pick up the traits it carries.
            children.addAll(gi.getTraits());
        }

        if (node instanceof LiftPronunciation p) {
            children.addAll(p.getMedias());
        }

        if (node instanceof AbstractExtensibleWithoutField a) {
            children.addAll(a.getAnnotations());
            children.addAll(a.getTraits());
            if (node instanceof HasField f) {
                children.addAll(f.getFields().values());
            }
        }

        if (node instanceof HasNote n) {
            children.addAll(n.getNotes().values());
        }

        if (node instanceof HasPronunciation p) {
            children.addAll(p.getPronunciations());
        }

        if (node instanceof HasRelations r) {
            children.addAll(r.getRelations());
        }

        if (node instanceof HasReversal r) {
            children.addAll(r.getReversals());
        }

        if (node instanceof LiftReversal r && r.getMain() != null) {
            // The <main> of a reversal is itself a LiftReversal, but it is held in a
            // field of its own rather than in getReversals().
            children.add(r.getMain());
        }

        if (node instanceof HasSense s) {
            children.addAll(s.getSenses());
        }

        return children;
    }

    // ------------------------------------------------------------------
    // Parent <-> child wiring
    // ------------------------------------------------------------------

    /**
     * Wire {@code child} into {@code parent}. The parent's {@code addX} method is
     * responsible for setting the back reference from the child to itself.
     */
    private static void wire(AbstractLiftRoot child, Object parent) {
        switch (child) {
            case LiftEntry _ -> {
                // A lexical entry is a root: it has no parent to wire.
            }
            case LiftNote note -> ((AbstractNotable) parent).addNote(note);
            case LiftSense sense -> ((HasSense) parent).addSense(sense);
            case LiftVariant variant -> ((LiftEntry) parent).addVariant(variant);
            case LiftPronunciation pronunciation ->
                ((HasPronunciation) parent).addPronunciation(pronunciation);
            case LiftExample example -> ((LiftSense) parent).addExample(example);
            case LiftField field -> ((HasField) parent).addField(field);
            case LiftAnnotation annotation ->
                ((HasAnnotation) parent).addAnnotation(annotation);
            case LiftTrait trait -> ((HasTrait) parent).addTrait(trait);
            case LiftRelation relation -> ((HasRelations) parent).addRelation(relation);
            case LiftEtymology etymology -> ((LiftEntry) parent).addEtymology(etymology);
            case LiftReversal reversal -> ((HasReversal) parent).addReversal(reversal);
            case GrammaticalInfo gi ->
                ((LiftSense) parent).setGrammaticalInfo(gi);
            default -> throw new IllegalArgumentException(
                "Unsupported element type: " + child
            );
        }
    }
}
