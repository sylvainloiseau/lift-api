package fr.cnrs.lacito.liftapi.model;

/**
 * A media file associated to a {@link LiftPronunciation} object.
 */
public final class LiftMedia
    extends AbstractLiftRoot
    implements HasExternalDocument
{

    protected String url;

    protected LiftPronunciation parent;

    public LiftPronunciation getParent() {
        return parent;
    }

    public LiftMedia(String href) {
        this.url = href;
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftMedia.
     * 
     * @param parent the new parent, or {@code null} when detaching this media
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(LiftPronunciation parent) {
        this.parent = parent;
    }

    @Override
    public String getHref() {
        return this.url;
    }

    @Override
    public MultiText getLabel() {
        return getMainMultiText();
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
