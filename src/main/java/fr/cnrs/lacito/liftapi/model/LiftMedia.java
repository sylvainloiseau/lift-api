package fr.cnrs.lacito.liftapi.model;

/**
 * A media file associated to a {@link LiftPronunciation} object.
 */
public final class LiftMedia
    extends AbstractLiftRoot
    implements ExternalDocument {

    protected String url;
    protected LiftPronunciation parent;

    protected LiftMedia(String href) {
        this.url = href;
    }

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

}
