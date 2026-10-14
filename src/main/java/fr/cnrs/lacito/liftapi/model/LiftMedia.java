package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;

/**
 * A media file associated to a {@link LiftPronunciation} object.
 */
public final class LiftMedia
    extends AbstractLiftRoot
    implements HasExternalDocument
{

    // Plain field until a property is asked for (see AbstractExtensibleWithoutField).
    private String href;
    private TrackedProperty<String> hrefProperty;

    protected LiftPronunciation parent;

    public LiftPronunciation getParent() {
        return parent;
    }

    public LiftMedia(String href) {
        this.href = href;
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
        return hrefProperty != null ? hrefProperty.get() : href;
    }

    /** The {@code href}; {@code null} when there is none. */
    public ObjectProperty<String> hrefProperty() {
        if (hrefProperty == null) {
            hrefProperty = new TrackedProperty<>(this, ModelProperty.HREF, href);
            href = null;
        }
        return hrefProperty;
    }

    public void setHref(String href) {
        if (hrefProperty != null) {
            hrefProperty.set(href);
        } else {
            String old = this.href;
            this.href = href;
            Changes.valueChanged(this, ModelProperty.HREF, old, href);
        }
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
