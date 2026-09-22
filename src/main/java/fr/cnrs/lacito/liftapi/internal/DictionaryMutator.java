package fr.cnrs.lacito.liftapi.internal;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasReversal;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

import java.util.ArrayList;
import java.util.List;

/**
 * The single place where a component is joined to a dictionary.
 *
 * Adding a component means doing two distinct things: wiring the parent and child
 * references to each other, and registering the component in the dictionary's
 * registry. There are also two ways components get created - the fluent
 * {@code builder} package and the lower-level {@code xml} factory used while parsing -
 * and they used to perform those two jobs independently, in different orders, with
 * their own copy of the "walk the children" logic. Every copy was free to drift from
 * the others, and they did: children attached before a failed registration left a
 * half-built graph, reversals were registered twice, and MultiTexts were unregistered
 * twice, corrupting the language counters.
 *
 * This class is the common core both paths now go through. In particular
 * {@link #childrenOf(AbstractLiftRoot)} is the <em>only</em> definition of what a
 * component's children are, so the add and remove traversals cannot disagree again.
 *
 * <h2>Not public API</h2>
 *
 * The package {@code fr.cnrs.lacito.liftapi.internal} is deliberately not exported by
 * {@code module-info.java}. The members here are {@code public} only because javac
 * requires it for access from the sibling packages inside this module; no consumer of
 * the library can reach them.
 */
public final class DictionaryMutator {

    private final LiftDictionaryRegistry registry;

    public DictionaryMutator(LiftDictionaryRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
    }

    // ------------------------------------------------------------------
    // Creation paths
    // ------------------------------------------------------------------

    /**
     * Add a freshly created component to the dictionary: used by the builders.
     *
     * The component is registered <em>before</em> it is wired to its parent, because
     * registration is what can fail (a duplicate LIFT id, for instance). Wiring first
     * would leave the parent holding a child the dictionary does not know about.
     *
     * @param child the newly created component
     * @param parent the component it belongs to; ignored for a {@link LiftEntry},
     *        which has no parent
     * @return {@code child}, for chaining
     */
    public <T extends AbstractLiftRoot> T attach(T child, Object parent) {
        registry.register(child);
        wire(child, parent);
        return child;
    }

    /**
     * Add an already-built subtree to the dictionary: used by the XML factory.
     *
     * The SAX reader assembles a whole {@code <entry>} - senses, examples, traits and
     * all - before the entry is complete, so it cannot register components one at a
     * time as the builders do. The parent and child references are already wired by
     * the time this is called; only registration remains.
     *
     * @param root the root of the subtree to adopt
     */
    public void adoptSubtree(AbstractLiftRoot root) {
        registry.addToDictionaryLowLevel(root);
    }

    // ------------------------------------------------------------------
    // The one traversal
    // ------------------------------------------------------------------

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
            case GrammaticalInfo gi ->
                ((LiftSense) parent).setGrammaticalInfo(gi);
            default -> throw new IllegalArgumentException(
                "Unsupported element type: " + child
            );
        }
    }
}
