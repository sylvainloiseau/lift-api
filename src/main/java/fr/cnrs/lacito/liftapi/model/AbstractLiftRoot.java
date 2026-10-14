package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.ComponentAdded;
import fr.cnrs.lacito.liftapi.change.ComponentRemoved;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Superclass of all LIFT component classes (all subclasses names are prefixed
 * with Lift: {@link LiftEntry}, etc.)
 *
 * This abstract class provide a Multitext field used by subclasses.
 * Subclasses are responsible for exposing this field
 * with the correct accessor. For instance {@code LiftEntry#getForms()}
 * refers to this field, while renamming it.
 *
 * <h2>Attached and detached components</h2>
 *
 * A component is <em>attached</em> when walking its parent chain
 * ({@link #getParentNode()}) reaches a {@link LiftEntry} that belongs to a dictionary;
 * otherwise it is <em>detached</em>. {@link #getOwningDictionary()} answers the
 * question, and it is the single rule governing registration: {@code addX()} registers
 * its argument, and {@code deleteX()} unregisters it, if and only if the receiver is
 * attached. See the {@code package-info} of this package for the full contract.
 *
 * <h2>Reporting changes</h2>
 *
 * Adding and removing components goes through {@link #adopted(AbstractLiftRoot)} and
 * {@link #removeChild(AbstractLiftRoot)} here, and {@link #detach()}: these are the
 * places that report {@link ComponentAdded} and {@link ComponentRemoved} to the
 * dictionary's listeners (see the {@code change} package).
 */
public abstract sealed class AbstractLiftRoot implements LiftObject
    permits
        AbstractExtensibleWithoutField,
        GrammaticalInfo,
        LiftAnnotation,
        LiftIllustration,
        LiftMedia,
        LiftTranslation,
        LiftTrait,
        LiftHeader,
        LiftFieldAndTraitDefinition,
        LiftReversal
{

    static {
        // Hand the internal wiring (setUUID, setOwningDictionary...) to the rest of the
        // module; see ModelInternals.
        ModelInternals.install(new InternalAccess());
    }

    private final MultiText mainMultiText = new MultiText(this);
    /** Allocated on the first unknown attribute: almost every component has none. */
    private Map<String, String> otherXmlAttributes;
    private UUID uuid;

    /**
     * The semantic of this multitext depends on the subclass.
     * @return
     */
    public MultiText getMainMultiText() {
        return mainMultiText;
    }

    /**
     * The XML attributes the reader found on this element and does not know.
     *
     * They are kept "just in case", and should normally be absent: nothing in the model
     * edits them, they are not reported as changes, and the writer does not write them
     * back.
     *
     * @return an unmodifiable map, empty in the normal case
     */
    public Map<String, String> getOtherXmlAttributes() {
        return otherXmlAttributes == null
            ? Map.of()
            : Collections.unmodifiableMap(otherXmlAttributes);
    }

    /** For the XML reader, through {@link ModelInternals}. */
    void putOtherXmlAttribute(String name, String value) {
        if (otherXmlAttributes == null) {
            otherXmlAttributes = new HashMap<>(4);
        }
        otherXmlAttributes.put(name, value);
    }

    protected void addToMainMultiText(Form t) {
        mainMultiText.add(t);
    }

    /**
     * The component this one belongs to, whatever its kind.
     *
     * Every subclass already stores a parent, but each under a getter of its own type
     * ({@link LiftSense#getParent()} returns a {@link HasSense},
     * {@link LiftExample#getParent()} returns a {@link LiftSense}, ...), which is
     * convenient for callers and useless for traversal. This narrows them all to a
     * single {@code AbstractLiftRoot}-returning accessor so that the chain from any
     * component up to its entry can be walked uniformly.
     *
     * @return the parent component, or {@code null} for a root ({@link LiftEntry},
     *         {@link LiftHeader}) or for a component that is not wired to a parent
     */
    public abstract AbstractLiftRoot getParentNode();

    /**
     * The object that holds this component: its parent component, or for an annotation
     * the {@link MultiText} or {@link Form} it annotates. The {@code parent} of the
     * change events.
     */
    Object holder() {
        return getParentNode();
    }

    /**
     * The dictionary this component belongs to, or {@code null} if it is detached.
     *
     * Only a {@link LiftEntry} knows its dictionary directly; every other component
     * finds it by walking up {@link #getParentNode()}. The walk is at most a handful of
     * links deep and allocates nothing.
     *
     * @return the owning dictionary, or {@code null} when this component is not (yet)
     *         part of one
     */
    public final LiftDictionary getOwningDictionary() {
        for (AbstractLiftRoot n = this; n != null; n = n.getParentNode()) {
            if (n instanceof LiftEntry e) {
                return e.owningDictionary();
            }
        }
        return null;
    }

    /**
     * The dictionary changes of this component are reported to: the owning dictionary
     * of an entry's subtree, as {@link #getOwningDictionary()}, but also the dictionary
     * of the header for the header's features, feature sets and definitions, which are
     * not registered in the indexes but are data all the same.
     */
    final LiftDictionary eventDictionary() {
        for (AbstractLiftRoot n = this; n != null; n = n.getParentNode()) {
            if (n instanceof LiftEntry e) {
                return e.owningDictionary();
            }
            if (n instanceof LiftHeader h) {
                return h.getDictionary();
            }
        }
        return null;
    }

    /**
     * Register {@code child} in this component's dictionary, if there is one, and report
     * its addition.
     *
     * Every {@code addX()} method ends with a call to this, and that is what closes the
     * gap between the two creation paths: wiring a component into an attached parent
     * registers it, so a component sitting in the tree that the registry does not know
     * about cannot happen. On a detached component this does nothing, which is what
     * lets the XML reader assemble a whole {@code <entry>} and adopt it in one go, and
     * what lets a subtree be cut and pasted.
     *
     * Registration is idempotent for a component already registered in this same
     * dictionary, so re-attaching a detached-but-still-registered subtree is free.
     * If registration fails (a duplicate LIFT id in the subtree, for instance), the
     * child is unwired again before the exception propagates: the addition did not
     * happen, and nothing is reported.
     *
     * @param child the component just wired into this one
     * @return {@code child}, for chaining
     */
    protected final <T extends AbstractLiftRoot> T adopted(T child) {
        adoptedIn(this, child);
        return child;
    }

    /**
     * {@link #adopted(AbstractLiftRoot)}, for a child wired into {@code holder}: this
     * component, or a {@link MultiText} or {@link Form} of it for an annotation.
     */
    final void adoptedIn(Object holder, AbstractLiftRoot child) {
        LiftDictionary dictionary = getOwningDictionary();
        boolean registers = dictionary != null && child.getUUID() == null;
        if (dictionary != null) {
            try {
                dictionary.getMutator().adoptSubtree(child);
            } catch (RuntimeException e) {
                child.unlink();
                throw e;
            }
        }
        LiftDictionary observer = Changes.observing(this);
        if (observer != null) {
            Changes.fire(observer, new ComponentAdded(
                observer, holder, child, child.positionInParent(), registers
            ));
        }
    }

    /**
     * Unregister {@code child} from this component's dictionary, if there is one.
     *
     * The mirror of {@link #adopted(AbstractLiftRoot)}: every {@code deleteX()} method
     * calls this before unlinking (through {@link #removeChild(AbstractLiftRoot)}), so
     * that a component cannot be taken out of the tree while staying in the dictionary's
     * indexes - the same defect as {@code addX()} leaving a component out of them, only
     * in reverse.
     *
     * Unregistering happens first precisely because it is the half that can fail: a
     * component still referred to from elsewhere is refused, and the tree is then left
     * exactly as it was rather than half-unlinked.
     *
     * @param child the component about to be unlinked from this one
     * @throws IllegalStateException if this component is detached but {@code child} is
     *         still registered somewhere, which no caller can mean
     */
    protected final void orphaned(AbstractLiftRoot child) {
        LiftDictionary dictionary = getOwningDictionary();
        if (dictionary != null) {
            dictionary.getMutator().releaseSubtree(child);
        } else if (child.getUUID() != null) {
            // The subtree was detached from its dictionary without being unregistered
            // (see detach()). Editing it in that state would strand the child in indexes
            // it can no longer be reached from.
            throw new IllegalStateException(
                "This " + getClass().getSimpleName() + " is detached but still " +
                    "registered: attach it again, or remove the whole subtree from its " +
                    "dictionary, before deleting anything from it."
            );
        }
    }

    /**
     * Delete {@code child} from this component: unregister it (see
     * {@link #orphaned(AbstractLiftRoot)}), unlink it, and report its removal. The body
     * of every {@code deleteX()} method, once it has checked that it holds the child.
     *
     * @param child a component held by this one
     */
    protected final void removeChild(AbstractLiftRoot child) {
        removeChildIn(child.holder(), child);
    }

    /**
     * {@link #removeChild(AbstractLiftRoot)}, for a child held by {@code holder}.
     */
    final void removeChildIn(Object holder, AbstractLiftRoot child) {
        LiftDictionary observer = Changes.observing(this);
        OptionalInt position = observer == null ? OptionalInt.empty() : child.positionInParent();
        boolean unregisters = getOwningDictionary() != null;
        orphaned(child);
        child.unlink();
        if (observer != null) {
            Changes.fire(observer, new ComponentRemoved(
                observer, holder, child, position, unregisters
            ));
        }
    }

    /**
     * Guard for the {@code deleteX} methods: a component can only be deleted from the
     * parent that actually holds it.
     *
     * Without this, {@code someEntry.deleteSense(aSenseOfAnotherEntry)} would
     * unregister a component that is still wired into a different part of the
     * dictionary.
     *
     * @param child the component the caller wants to delete
     * @param held whether this component currently holds it
     */
    protected final void requireChild(AbstractLiftRoot child, boolean held) {
        if (child == null) {
            throw new IllegalArgumentException(
                "the component to delete cannot be null"
            );
        }
        if (!held) {
            throw new IllegalArgumentException(
                child.getClass().getSimpleName() + " is not held by this " +
                    getClass().getSimpleName() + ": it cannot be deleted from it."
            );
        }
    }

    /**
     * Set by the dictionary when it registers this component, and cleared when it
     * unregisters it. Package-private: the mutator reaches it through
     * {@link ModelInternals}.
     */
    void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * The position of this component in its parent's list.
     *
     * The order of the components held in a list (senses, examples, traits, relations,
     * variants...) is meaningful; this is what a removal records so that it can be
     * undone in place. For an entry, it is its position among the dictionary's entries.
     *
     * @return the index, or empty for a component held in a map (a note, a field, a
     *         translation), in a single slot (a grammatical info, the main of a
     *         reversal), or without a parent
     */
    public final OptionalInt positionInParent() {
        if (this instanceof LiftEntry e) {
            LiftDictionary d = e.owningDictionary();
            return d == null
                ? OptionalInt.empty()
                : indexIn(d.getLiftDictionaryRegistry().getEntries());
        }
        if (getParentNode() == null) {
            return OptionalInt.empty();
        }
        return switch (this) {
            case LiftSense s -> switch (s.getParent()) {
                case LiftEntry p -> indexIn(p.senses);
                case LiftSense p -> indexIn(p.subSenses);
            };
            case LiftExample o -> indexIn(o.getParent().examples);
            case LiftVariant o -> indexIn(o.getParent().variants);
            case LiftEtymology o -> indexIn(o.getParent().etymologies);
            case LiftIllustration o -> indexIn(o.getParent().illustrations);
            case LiftMedia o -> indexIn(o.getParent().medias);
            case LiftPronunciation o -> switch (o.getParent()) {
                case LiftEntry p -> indexIn(p.pronunciations);
                case LiftVariant p -> indexIn(p.pronunciations);
            };
            case LiftRelation o -> switch (o.getParent()) {
                case LiftEntry p -> indexIn(p.relations);
                case LiftSense p -> indexIn(p.relations);
                case LiftVariant p -> indexIn(p.relations);
            };
            case LiftReversal o -> switch (o.getParent()) {
                case LiftSense p -> indexIn(p.reversals);
                case LiftReversal p -> p.getMain() == o
                    ? OptionalInt.empty()
                    : indexIn(p.reversals);
            };
            case LiftTrait o -> switch (o.getParent()) {
                case AbstractExtensibleWithoutField p -> indexIn(p.traits);
                case GrammaticalInfo p -> indexIn(p.traits);
            };
            case LiftAnnotation o -> switch (o.getParent()) {
                case AbstractExtensibleWithoutField p -> indexIn(p.annotations);
                case LiftTrait p -> indexIn(p.annotations);
                case MultiText p -> indexIn(p.annotationsOrEmpty());
                case Form p -> indexIn(p.annotationsOrEmpty());
            };
            default -> OptionalInt.empty();
        };
    }

    private OptionalInt indexIn(List<?> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == this) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Detach this node from its parent: remove the link parent -&gt; self, and set the
     * parent of this node to null.
     *
     * Detaching does <em>not</em> unregister: the node keeps its UUID and stays in the
     * dictionary's indexes, so that it can be attached again elsewhere in the same
     * dictionary (a move, an undo) without going through registration twice. To take a
     * subtree out of a dictionary entirely - to delete it, or to hand it to another
     * dictionary - call the matching {@code deleteX} on its parent
     * ({@code sense.deleteExample(example)}), or
     * {@link fr.cnrs.lacito.liftapi.LiftDictionary#removeEntry(LiftEntry)} for an entry.
     * Those unregister as well as unlink.
     *
     * Reported as a {@link ComponentRemoved} whose {@code unregistered} is {@code false}.
     *
     * Detaching a node that has no parent is a no-op.
     */
    public void detach() {
        if (getParentNode() == null) {
            // A root (an entry, the header), or a node already detached.
            return;
        }
        Object holder = holder();
        LiftDictionary observer = Changes.observing(this);
        OptionalInt position = observer == null ? OptionalInt.empty() : positionInParent();
        unlink();
        if (observer != null) {
            Changes.fire(observer, new ComponentRemoved(observer, holder, this, position, false));
        }
    }

    /**
     * Remove the links between this node and its parent, both ways, without reporting
     * anything: the common part of {@link #detach()} and of the {@code deleteX} methods.
     */
    final void unlink() {
        switch (this) {
            case LiftSense s -> {
                switch (s.getParent()) {
                    case LiftEntry p -> p.senses.removeIf(x -> x == s);
                    case LiftSense p -> p.subSenses.removeIf(x -> x == s);
                    case null -> { }
                }
                s.setParent(null);
            }
            case LiftExample o -> {
                if (o.getParent() != null) o.getParent().examples.removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftVariant o -> {
                if (o.getParent() != null) o.getParent().variants.removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftTrait o -> {
                switch (o.getParent()) {
                    case AbstractExtensibleWithoutField p -> p.traits.removeIf(x -> x == o);
                    case GrammaticalInfo p -> p.traits.removeIf(x -> x == o);
                    case null -> { }
                }
                o.setParent(null);
            }
            case LiftReversal o -> {
                switch (o.getParent()) {
                    case LiftSense p -> p.reversals.removeIf(x -> x == o);
                    case LiftReversal p -> {
                        // A reversal is held either in its parent's list or in its <main> slot.
                        if (!p.reversals.removeIf(x -> x == o) && p.getMain() == o) {
                            p.clearMain();
                        }
                    }
                    case null -> { }
                }
                o.setParent(null);
            }
            case LiftRelation o -> {
                switch (o.getParent()) {
                    case LiftEntry p -> p.relations.removeIf(x -> x == o);
                    case LiftSense p -> p.relations.removeIf(x -> x == o);
                    case LiftVariant p -> p.relations.removeIf(x -> x == o);
                    case null -> { }
                }
                o.setParent(null);
            }
            case LiftPronunciation o -> {
                switch (o.getParent()) {
                    case LiftEntry p -> p.pronunciations.removeIf(x -> x == o);
                    case LiftVariant p -> p.pronunciations.removeIf(x -> x == o);
                    case null -> { }
                }
                o.setParent(null);
            }
            case LiftNote o -> {
                if (o.getParent() != null && o.getParent().notes.get(o.getType()) == o) {
                    o.getParent().notes.remove(o.getType());
                }
                o.setParent(null);
            }
            case LiftMedia o -> {
                if (o.getParent() != null) o.getParent().medias.removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftIllustration o -> {
                if (o.getParent() != null) o.getParent().illustrations.removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftField o -> {
                String name = o.getSpecification().getName();
                if (o.getParent() != null && o.getParent().fields.get(name) == o) {
                    o.getParent().fields.remove(name);
                }
                o.setParent(null);
            }
            case LiftEtymology o -> {
                if (o.getParent() != null) o.getParent().etymologies.removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftAnnotation o -> {
                switch (o.getParent()) {
                    case AbstractExtensibleWithoutField p -> p.annotations.removeIf(x -> x == o);
                    case LiftTrait p -> p.annotations.removeIf(x -> x == o);
                    case MultiText p -> p.annotations().removeIf(x -> x == o);
                    case Form p -> p.annotations().removeIf(x -> x == o);
                    case null -> { }
                }
                o.setParent(null);
            }
            case LiftTranslation o -> {
                if (o.getParent() != null) o.getParent().removeTranslation(o);
                o.setParent(null);
            }
            case GrammaticalInfo o -> {
                if (o.getParent() != null) o.getParent().clearGrammaticalInfo();
                o.setParent(null);
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + this.getClass()
            );
        }
    }
}
