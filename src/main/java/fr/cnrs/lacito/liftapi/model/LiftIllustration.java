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

    public LiftSense getParent() {
        return parent;
    }

    public LiftIllustration(String href) {
        this.href = href;
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
}
