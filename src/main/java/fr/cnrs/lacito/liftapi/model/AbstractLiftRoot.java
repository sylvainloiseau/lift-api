package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Superclass of all LIFT component classes (all subclasses names are prefixed with Lift: {@link LiftEntry}, etc.)
 *
 * This abstract class provide a Multitext field used by subclasses.
 * Subclasses are responsible for exposing this field
 * with the correct accessor. For instance {@code LiftEntry#getForms()}
 * refers to this field, while renamming it.
 */
public abstract sealed class AbstractLiftRoot implements LiftObject
    permits
        AbstractExtensibleWithoutField,
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

    //public abstract void setParent(AbstractLiftRoot parent);

    // TODO should be protected, but it is used in the builder, which is in another package. We should move the builder to the same package as the model
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * Detach this node from its parent: remove the link parent -> self, and set the parent of this node to null.
     */
    public void detach() {
        switch (this) {
            case LiftEntry _ -> {
                // do nothing: does not have a parent.
            }
            case LiftSense s -> {
                HasSense p = s.getParent();
                p.getSenses().removeIf(x -> x == s);
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
            case LiftReversal o -> { o.getParent()
                .getReversals()
                .removeIf(x -> x == o);
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
            case LiftAnnotation o -> { o.getParent()
                .getAnnotations()
                .removeIf(x -> x == this);
                o.setParent(null);
            }
            default -> throw new IllegalStateException(
                "Unknown type: " + this.getClass()
            );
        }
    }
}
