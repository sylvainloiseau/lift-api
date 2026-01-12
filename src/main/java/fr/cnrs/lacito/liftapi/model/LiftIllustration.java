package fr.cnrs.lacito.liftapi.model;

/**
 * A still image associated with a {@link LiftSense LiftSense}.
 */
public final class LiftIllustration 
    extends AbstractLiftRoot
    implements ExternalDocument {

    final String href;
    LiftSense parent;

    protected LiftIllustration (String href) {
        this.href = href;
    }

    public void setParent(LiftSense parent) {
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
