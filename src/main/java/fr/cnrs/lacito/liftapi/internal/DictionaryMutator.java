package fr.cnrs.lacito.liftapi.internal;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementBuilder;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
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
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <li> <em>attaching</em> means wiring a freshly built component to its parent with the
 * parent's {@code addX} (see {@link #attach(AbstractLiftRoot, Object)}). It is what a
 * builder's {@code build()} does. Its sub-components were attached to it before, by the
 * nested builders. Whether anything is registered then follows the one rule below.</li>
 * <li> <em>adopting / releasing</em> means recursively registering (unregistering) a
 * component already wired to its parent, with everything under it; adoption is
 * idempotent for the components this dictionary already holds.</li>
 * </ul>
 *
 * The one rule: a subtree is registered the first time it is wired into an attached
 * parent, and not before. A builder building into an attached parent registers its
 * component at once; nested builders build into a parent that is not attached yet, so
 * nothing is registered until the outermost component is attached, and the single
 * adoption it triggers registers the whole subtree. The XML reader relies on it: it
 * builds every component of an {@code <entry>} into the detached entry, and the entry's
 * own {@code build()} registers it all at once.
 *
 * The components of the header (feature sets, features, definitions) are not
 * registered; their texts are, as meta-language texts, when they join the header (see
 * {@link #registerHeaderTexts(AbstractLiftRoot)}).
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

    /** The model's package-private wiring (setUUID, setOwningDictionary...). */
    private static final ModelInternals.Access MODEL = ModelInternals.get();

    private final DictionaryRegisters registers;

    /**
     * The dictionary being mutated.
     *
     * Held so that registration can reach its language managers and stamp each entry
     * with its owner, which is what {@code AbstractLiftRoot.getOwningDictionary()}
     * resolves against.
     */
    private final LiftDictionary owner;

    /** The warnings of the reader about the definitions of the file (see {@code LiftDictionary.getLoadWarnings()}). */
    private final List<String> loadWarnings = new ArrayList<>();

    /** Records a warning of the reader. */
    public void recordLoadWarning(String message) {
        loadWarnings.add(message);
    }

    /** The warnings of the reader, read-only. */
    public List<String> loadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

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
     * Wire a freshly created component to its parent: used by the builders
     * ({@code AbstractLiftElementBuilder.attach()}).
     *
     * The component has no UUID yet. Its sub-components were attached to it before, by
     * the nested builders, so this does not recurse; the registration it triggers, when
     * the parent is attached, covers the whole subtree.
     *
     * The component is wired to its parent with the parent's {@code addX} method, which
     * registers it when the parent is attached (see {@code AbstractLiftRoot.adopted}),
     * and reports the addition: a builder goes through the same path as any other
     * caller. Registration is all-or-nothing, and a failed one (a duplicate LIFT id, for
     * instance) unwires the child again, so the parent is never left holding a child the
     * dictionary does not know about. An entry is added with
     * {@link LiftDictionary#addEntry(LiftEntry)}.
     *
     * @param child the newly created component
     * @param parent the component it belongs to; ignored for a {@link LiftEntry},
     *        which has no parent
     * @return {@code child}, for chaining
     */
    public <T extends AbstractLiftRoot> T attach(T child, Object parent) {
        if (child instanceof LiftEntry e) {
            owner.addEntry(e);
        } else {
            wire(child, parent);
        }
        return child;
    }

    /**
     * Set a freshly created reversal as the {@code <main>} of another: the one slot
     * whose child the type of the component does not designate, since a
     * {@link LiftReversal} also goes into the list of reversals of its parent (see
     * {@link #attach(AbstractLiftRoot, Object)}).
     *
     * @param main the newly created reversal
     * @param parent the reversal it is the main of; its current main is deleted
     */
    public void attachMain(LiftReversal main, LiftReversal parent) {
        parent.setMain(main);
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
     * a whole {@code <entry>} read from a file takes: the XML reader builds its senses,
     * examples, traits and all into the entry before the entry itself is built, so none
     * of those builds registers anything, and this single traversal, when the entry is
     * added, does all the work.
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
        // All or nothing: a failure part-way (a duplicate LIFT id deep in the subtree)
        // takes back what was registered before it.
        List<AbstractLiftRoot> registered = new ArrayList<>();
        try {
            adoptRecursively(root, registered);
        } catch (RuntimeException e) {
            rollback(registered);
            throw e;
        }
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

    private void adoptRecursively(AbstractLiftRoot node, List<AbstractLiftRoot> registered) {
        // 1. register the node and its MultiText(s), unless we already hold it
        if (!isRegisteredHere(node)) {
            register(node);
            registered.add(node);
        }
        // 2. recursively add its descendants, using the same child enumeration as
        // releaseSubtree so the two stay mirror images.
        for (AbstractLiftRoot child : childrenOf(node)) {
            adoptRecursively(child, registered);
        }
    }

    /**
     * Take back the registration of the nodes of a failed adoption, last first.
     */
    private void rollback(List<AbstractLiftRoot> registered) {
        for (int i = registered.size() - 1; i >= 0; i--) {
            AbstractLiftRoot node = registered.get(i);
            if (node instanceof HasRefId source) {
                String target = referenceTarget(source);
                if (target != null) {
                    registers.removeReference(target, source);
                }
            }
            unregister(node);
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
     * <li>giving it a LIFT id (entries and senses only) if it does not have one: a
     * random UUID of its own, not the internal one, since the LIFT id is written to the
     * file and must not collide with the ids of other files or sessions, which the
     * internal UUIDs, unique only within this dictionary, would (see
     * {@link LiftDictionaryUUIDManager})</li>
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

        // Only entries and senses carry a LIFT id of their own. Everything that can fail
        // is checked before anything is changed, so that a refusal leaves no trace.
        // The default id is set before the UUID, as on any unregistered component.
        switch (node) {
            case LiftEntry e -> {
                if (e.getId().isEmpty()) {
                    e.setId(UUID.randomUUID().toString());
                }
                if (registers.entriesByLiftId.containsKey(e.getId().get())) {
                    throw new DuplicateIdException(
                        "Duplicate lift id: " + e.getId().get()
                    );
                }
            }
            case LiftSense s -> {
                if (s.getId().isEmpty()) {
                    s.setId(UUID.randomUUID().toString());
                }
                // Same guard as for entries: without it a file with two senses
                // sharing an id silently loses one of them.
                if (registers.sensesByLiftId.containsKey(s.getId().get())) {
                    throw new DuplicateIdException(
                        "Duplicate lift id: " + s.getId().get()
                    );
                }
            }
            default -> {
            }
        }

        MODEL.setUUID(node, uuid);
        registers.nodesById(node).put(uuid, node);

        switch (node) {
            case LiftEntry e -> {
                // The one back reference from the component graph to the dictionary:
                // everything below this entry resolves getOwningDictionary() through it.
                MODEL.setOwningDictionary(e, owner);
                registers.entriesByLiftId.put(e.getId().get(), e);
                registers.entryLiftId2Uuid.put(e.getId().get(), uuid);
                registers.entries.add(e);
            }
            case LiftSense s -> {
                registers.sensesByLiftId.put(s.getId().get(), s);
                registers.senseLiftId2Uuid.put(s.getId().get(), uuid);
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
            String target = referenceTarget(hasref);
            if (target != null) {
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
        MODEL.setUUID(element, uuid);
        textById.put(uuid, element);
        for (String lang : element.getLangs()) {
            if (!languagesManager.hasLanguage(lang)) {
                languagesManager.addLanguage(lang);
            }
        }
        MODEL.setLanguagesManager(element, languagesManager);
    }

    // ------------------------------------------------------------------
    // The texts of the header
    // ------------------------------------------------------------------

    /**
     * Register the MultiTexts of a component of the header, and of the components under
     * it, as meta-language texts.
     *
     * The components of the header - feature sets, features, field and trait
     * definitions, and the header itself - are not indexed and get no UUID: they are
     * reached through {@code LiftHeader}, report their own changes, and nothing needs to
     * look them up by UUID. Their labels, abbreviations and descriptions are text all the
     * same, in the meta-languages of the dictionary, and registering them gives them
     * what every other meta-language text has: their languages are declared and counted
     * in {@code getMetaLanguageManager()}, and a form in a language the dictionary does
     * not declare is refused.
     *
     * Called by the header when a component joins it (and for the whole header when it
     * joins its dictionary). A text that is already registered is left as it is, so
     * calling this again - when a feature set is put back by an undo, say - is harmless.
     *
     * @param node the header, a feature set, a feature or a definition
     */
    public void registerHeaderTexts(AbstractLiftRoot node) {
        for (MultiText text : headerTextsOf(node)) {
            if (text.getUUID() == null) {
                registerMultiText(text, registers.metaTextById, owner.getMetaLanguageManager());
            }
        }
        for (AbstractLiftRoot child : headerChildrenOf(node)) {
            registerHeaderTexts(child);
        }
    }

    /**
     * Unregister the MultiTexts of a component of the header, and of the components under
     * it: the mirror of {@link #registerHeaderTexts(AbstractLiftRoot)}, called when the
     * component leaves the header.
     *
     * @param node the header, a feature set, a feature or a definition
     */
    public void releaseHeaderTexts(AbstractLiftRoot node) {
        for (MultiText text : headerTextsOf(node)) {
            if (text.getUUID() != null && registers.metaTextById.get(text.getUUID()) == text) {
                unregisterMultiText(text, registers.metaTextById);
            }
        }
        for (AbstractLiftRoot child : headerChildrenOf(node)) {
            releaseHeaderTexts(child);
        }
    }

    /**
     * The MultiTexts of a component of the header, all of them meta-language texts.
     *
     * @param node the header, a feature set, a feature or a definition
     * @return its texts
     */
    public static List<MultiText> headerTextsOf(AbstractLiftRoot node) {
        return switch (node) {
            case LiftHeader h -> List.of(h.getDescription());
            case FeatureSet fs -> List.of(fs.getLabel(), fs.getAbbrev(), fs.getDescription());
            case Feature f -> List.of(f.getLabel(), f.getAbbrev(), f.getDescription());
            case LiftFieldAndTraitDefinition d -> List.of(d.getLabel(), d.getDescription());
            default -> throw new IllegalStateException(
                "Not a component of the header: " + node.getClass()
            );
        };
    }

    /** The components of the header held by {@code node}. */
    private static List<AbstractLiftRoot> headerChildrenOf(AbstractLiftRoot node) {
        List<AbstractLiftRoot> children = new ArrayList<>();
        switch (node) {
            case LiftHeader h -> {
                children.addAll(h.getFeatureSets());
                children.addAll(h.getFieldsAndTraitsDefinitions());
            }
            case FeatureSet fs -> children.addAll(fs.getFeatures().values());
            default -> {
            }
        }
        return children;
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
        // All or nothing: check the whole subtree before changing anything. A component
        // may only be referred to by relations and variants of the subtree visited
        // before it (an entry's own relations to its senses, for instance), whose
        // references are released first.
        List<AbstractLiftRoot> nodes = new ArrayList<>();
        collect(root, nodes);
        Set<HasRefId> released = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractLiftRoot node : nodes) {
            if (node instanceof HasRefId source) {
                released.add(source);
            } else if (node instanceof AbstractIdentifiable i) {
                String id = i.getId().orElse(null);
                if (id != null) {
                    for (HasRefId source : registers.referencesTo(id)) {
                        if (!released.contains(source)) {
                            throw new IllegalStateException(
                                "Cannot delete this node: it is referenced from other nodes."
                            );
                        }
                    }
                }
            }
        }
        for (AbstractLiftRoot node : nodes) {
            if (node instanceof HasRefId source) {
                String target = referenceTarget(source);
                if (target != null) {
                    registers.removeReference(target, source);
                }
            }
            // unregister() also unregisters the node's own MultiTexts.
            unregister(node);
        }
    }

    /** The nodes of a subtree, in the traversal order of {@link #childrenOf}. */
    private static void collect(AbstractLiftRoot node, List<AbstractLiftRoot> nodes) {
        nodes.add(node);
        for (AbstractLiftRoot child : childrenOf(node)) {
            collect(child, nodes);
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

        MODEL.setUUID(node, null);

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
            MODEL.setOwningDictionary(e, null);
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
        MODEL.unregister(element);
        MODEL.setUUID(element, null);
        MODEL.setLanguagesManager(element, null);
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
                 LiftAnnotation _, LiftTranslation _ -> List.of();
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
            case LiftTranslation t -> List.of(t.getTranslation());
            case LiftRelation r -> List.of(r.getUsage());
            case LiftNote n -> List.of(n.getText());
            case LiftMedia m -> List.of(m.getLabel());
            case LiftIllustration i -> List.of(i.getLabel());
            case LiftField f -> List.of(f.getText());
            case LiftAnnotation a -> List.of(a.getText());
            case LiftEntry _, LiftExample _, LiftVariant _, LiftTrait _,
                 GrammaticalInfo _, LiftReversal _, LiftPronunciation _ -> List.of();
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

        if (node instanceof LiftExample x) {
            children.addAll(x.getTranslationComponents());
        }

        if (node instanceof GrammaticalInfo gi) {
            // GrammaticalInfo is not an AbstractExtensibleWithoutField, so the branch
            // below does not pick up the traits it carries.
            children.addAll(gi.getTraits());
        }

        if (node instanceof LiftPronunciation p) {
            children.addAll(p.getMedias());
        }

        if (node instanceof LiftTrait t) {
            // A trait is not an AbstractExtensibleWithoutField either.
            children.addAll(t.getAnnotations());
        }

        // The annotations held by the node's texts, and by their forms.
        for (MultiText text : objectTextsOf(node)) {
            textAnnotations(text, children);
        }
        for (MultiText text : metaTextsOf(node)) {
            textAnnotations(text, children);
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

    private static void textAnnotations(MultiText text, List<AbstractLiftRoot> children) {
        // hasAnnotations() first: getAnnotations() would allocate a list on every text.
        if (text.hasAnnotations()) {
            children.addAll(text.getAnnotations());
        }
        for (Form f : text.getForms()) {
            if (f.hasAnnotations()) {
                children.addAll(f.getAnnotations());
            }
        }
    }

    // ------------------------------------------------------------------
    // References and ids
    // ------------------------------------------------------------------

    /**
     * The LIFT id a relation or a variant refers to, as the reference counts know it:
     * its {@code ref} if it has one, otherwise the id of the component it points to.
     *
     * @return the id, or {@code null} when it refers to nothing
     */
    public static String referenceTarget(HasRefId source) {
        String target = source.getRefId().orElse(null);
        if (target == null && source.getRefObject() != null) {
            target = source.getRefObject().getId().orElse(null);
        }
        return target == null || target.isBlank() ? null : target;
    }

    /**
     * Move the reference count of {@code source} from {@code oldTarget} to
     * {@code newTarget}, after it changed what it refers to.
     */
    public void retarget(HasRefId source, String oldTarget, String newTarget) {
        if (java.util.Objects.equals(oldTarget, newTarget)) {
            return;
        }
        if (oldTarget != null) {
            registers.removeReference(oldTarget, source);
        }
        if (newTarget != null) {
            registers.addReference(newTarget, source);
        }
    }

    /**
     * Rename a registered entry or sense in the LIFT-id indexes. Called by
     * {@code setId} before it changes the id.
     *
     * @throws DuplicateIdException if another entry (or sense) has {@code newId}
     * @throws IllegalStateException if relations or variants refer to {@code oldId}
     */
    public void changeLiftId(AbstractIdentifiable node, String oldId, String newId) {
        Map<String, ? extends AbstractIdentifiable> byId = node instanceof LiftEntry
            ? registers.entriesByLiftId
            : registers.sensesByLiftId;
        Map<String, UUID> toUuid = node instanceof LiftEntry
            ? registers.entryLiftId2Uuid
            : registers.senseLiftId2Uuid;
        if (byId.containsKey(newId)) {
            throw new DuplicateIdException("Duplicate lift id: " + newId);
        }
        if (oldId != null && registers.isReferenced(oldId)) {
            throw new IllegalStateException(
                "Cannot change the id " + oldId + ": it is referenced from other nodes."
            );
        }
        if (oldId != null) {
            byId.remove(oldId);
            toUuid.remove(oldId);
        }
        switch (node) {
            case LiftEntry e -> registers.entriesByLiftId.put(newId, e);
            case LiftSense s -> registers.sensesByLiftId.put(newId, s);
        }
        toUuid.put(newId, node.getUUID());
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
            case LiftTranslation translation ->
                ((LiftExample) parent).addTranslation(translation);
            case LiftField field -> ((HasField) parent).addField(field);
            case LiftAnnotation annotation ->
                ((HasAnnotation) parent).addAnnotation(annotation);
            case LiftTrait trait -> ((HasTrait) parent).addTrait(trait);
            case LiftRelation relation -> ((HasRelations) parent).addRelation(relation);
            case LiftEtymology etymology -> ((LiftEntry) parent).addEtymology(etymology);
            // The list of reversals: the <main> slot has its own method, attachMain.
            case LiftReversal reversal -> ((HasReversal) parent).addReversal(reversal);
            case GrammaticalInfo gi ->
                ((LiftSense) parent).setGrammaticalInfo(gi);
            case LiftMedia media -> ((LiftPronunciation) parent).addMedia(media);
            case LiftIllustration illustration ->
                ((LiftSense) parent).addIllustration(illustration);
            default -> throw new IllegalArgumentException(
                "Unsupported element type: " + child
            );
        }
    }
}
