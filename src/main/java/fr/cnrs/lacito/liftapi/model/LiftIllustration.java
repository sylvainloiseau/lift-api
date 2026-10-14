package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;

/**
 * A still image associated with a {@link LiftSense LiftSense}.
 */
public final class LiftIllustration
    extends AbstractLiftRoot
    implements HasExternalDocument
{

    // Plain field until a property is asked for (see AbstractExtensibleWithoutField).
    private String href;
    private TrackedProperty<String> hrefProperty;

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
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
