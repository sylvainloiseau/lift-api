package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionary;

import java.util.HashMap;
import java.util.Map;
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
 */
public abstract sealed class AbstractLiftRoot implements LiftObject
    permits
        AbstractExtensibleWithoutField,
        GrammaticalInfo,
        LiftAnnotation,
        LiftIllustration,
        LiftMedia,
        LiftTrait,
        LiftHeader,
        LiftFieldAndTraitDefinition,
        LiftReversal
{

    private final MultiText mainMultiText = new MultiText(this);
    protected final Map<String, String> otherXmlAttributes = new HashMap<>();
    private UUID uuid;

    /**
     * The semantic of this multitext depends on the subclass.
     * @return
     */
    public MultiText getMainMultiText() {
        return mainMultiText;
    }

    public Map<String, String> getOtherXmlAttributes() {
        return otherXmlAttributes;
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
     * Register {@code child} in this component's dictionary, if there is one.
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
     *
     * @param child the component just wired into this one
     * @return {@code child}, for chaining
     */
    protected final <T extends AbstractLiftRoot> T adopted(T child) {
        LiftDictionary dictionary = getOwningDictionary();
        if (dictionary != null) {
            dictionary.getMutator().adoptSubtree(child);
        }
        return child;
    }

    /**
     * Unregister {@code child} from this component's dictionary, if there is one.
     *
     * The mirror of {@link #adopted(AbstractLiftRoot)}: every {@code deleteX()} method
     * calls this before unlinking, so that a component cannot be taken out of the tree
     * while staying in the dictionary's indexes - the same defect as {@code addX()}
     * leaving a component out of them, only in reverse.
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

    // TODO should be protected, but it is used in the builder, which is in another package. We should move the builder to the same package as the model
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
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
     * Detaching a node that has no parent is a no-op.
     */
    public void detach() {
        if (getParentNode() == null) {
            // A root (an entry, the header), or a node already detached.
            return;
        }
        switch (this) {
            case LiftSense s -> {
                s.getParent().getSenses().removeIf(x -> x == s);
                s.setParent(null);
            }
            case LiftExample o -> {o.getParent()
                .getExamples()
                .removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftVariant o -> { o.getParent()
                .getVariants()
                .removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftTrait o -> { o.getParent()
                .getTraits()
                .removeIf(x -> x == o);
                o.setParent(null);
            }
            case LiftReversal o -> {
                // A reversal is held either in its parent's list or in its <main> slot.
                boolean wasInList = o.getParent().getReversals().removeIf(x -> x == o);
                if (!wasInList
                    && o.getParent() instanceof LiftReversal r
                    && r.getMain() == o) {
                    r.setMain(null);
                }
                o.setParent(null);
            }
            case LiftRelation o -> {
                o.getParent()
                .getRelations().
                removeIf(x -> x == this);
                o.setParent(null);
            }
            case LiftPronunciation o -> { o.getParent()
                .getPronunciations()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            case LiftNote o -> { o.getParent()
                .getNotes()
                .remove(o.getType().getId());
                o.setParent(null);
            }
            case LiftMedia o -> { o.getParent()
                .getMedias()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            case LiftIllustration o -> { o.getParent()
                .getIllustrations()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            case LiftField o -> { o.getParent()
                .getFields()
                .remove(o.getSpecification().getName());
                o.setParent(null);
            }
            case LiftEtymology o -> { o.getParent()
                .getEtymologies()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            case LiftAnnotation o -> {
                o.getParent()
                .getAnnotations()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            case GrammaticalInfo o -> {
                o.getParent().clearGrammaticalInfo();
                o.setParent(null);
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + this.getClass()
            );
        }
    }
}
