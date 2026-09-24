package fr.cnrs.lacito.liftapi.model;

/**
 * A still image associated with a {@link LiftSense LiftSense}.
 */
public final class LiftIllustration
    extends AbstractLiftRoot
    implements HasExternalDocument
{

    final String href;

    LiftSense parent;

    public LiftIllustration(String href) {
        this.href = href;
    }

    public LiftSense getParent() {
        return parent;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftIllustration.
     * 
     * @param parent the new parent, or {@code null} when detaching this illustration
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(LiftSense parent) {
        this.parent = parent;
    }

    @Override
    public MultiText getLabel() {
        return getMainMultiText();
    }

    @Override
    public String getHref() {
        return this.href;
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
