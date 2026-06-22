package fr.cnrs.lacito.liftapi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Root of the hierarchy for Lift objects.
 * All the concret subclasses of this class have a name starting with "Lift": LiftSense, LiftEntry, etc.
 *
 * This abstract class provide a Multitext field used by subclasses.
 * Subclasses are responsible for exposing this field
 * with the correct accessor. For instance {@code LiftEntry#getForms()}
 * refers to this field, while renamming it.
 */
public abstract sealed class AbstractLiftRoot
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

    private final MultiText mainMultiText = new MultiText();
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
     * Detach this node from its parent.
     */
    public void detach() {
        switch (this) {
            case LiftEntry e -> {
                // do nothing: does not have a parent.
            }
            case LiftSense s -> {
                HasSense p = s.getParent();
                p.getSenses().removeIf(x -> x == s);
            }
            case LiftExample o -> o.getParent()
                .getExamples()
                .removeIf(x -> x == o);
            case LiftVariant o -> o.getParent()
                .getVariants()
                .removeIf(x -> x == o);
            case LiftTrait o -> o.getParent()
                .getTraits()
                .removeIf(x -> x == o);
            case LiftReversal o -> o.getParent()
                .getReversals()
                .removeIf(x -> x == o);
            case LiftRelation o -> {
                HasRelations r = o.getParent();
                r.getRelations().removeIf(x -> x == this);
            }
            case LiftPronunciation o -> o.getParent()
                .getPronunciations()
                .removeIf(x -> x == this);
            case LiftNote o -> o.getParent()
                .getNotes()
                .remove(o.getType().orElse(""));
            case LiftMedia o -> o.getParent()
                .getMedias()
                .removeIf(x -> x == this);
            case LiftIllustration o -> o.getParent()
                .getIllustrations()
                .removeIf(x -> x == this);
            case LiftField o -> o.getParent()
                .getFields()
                .removeIf(x -> x == this);
            case LiftEtymology o -> o.getParent()
                .getEtymologies()
                .removeIf(x -> x == this);
            case LiftAnnotation o -> o.getParent()
                .getAnnotations()
                .removeIf(x -> x == this);
            default -> throw new IllegalStateException(
                "Unknown type: " + this.getClass()
            );
        }
    }
}
