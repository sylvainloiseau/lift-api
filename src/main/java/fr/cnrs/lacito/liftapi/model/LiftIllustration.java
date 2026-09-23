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
