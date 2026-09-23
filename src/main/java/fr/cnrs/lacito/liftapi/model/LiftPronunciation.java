package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public final class LiftPronunciation extends AbstractExtensibleWithField {

    protected final ListProperty<LiftMedia> mediasProperty =
        new SimpleListProperty<>(
            this,
            "medias",
            FXCollections.observableArrayList()
        );

    private HasPronunciation parent;

    public HasPronunciation getParent() {
        return parent;
    }

    public LiftPronunciation() {}

    public MultiText getPronunciation() {
        return getMainMultiText();
    }

    protected void setParent(HasPronunciation parent) {
        this.parent = parent;
    }

    public List<LiftMedia> getMedias() {
        return mediasProperty.get();
    }

    public void addMedia(LiftMedia m) {
        mediasProperty.add(m);
        m.setParent(this);
        adopted(m);
    }

    public ListProperty<LiftMedia> mediasProperty() {
        return mediasProperty;
    }

    public static LiftPronunciation create() {
        return new LiftPronunciation();
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a media from this pronunciation, unregistering it if this pronunciation
     * belongs to a dictionary.
     *
     * @param m a media of this pronunciation
     */
    public void deleteMedia(LiftMedia m) {
        requireChild(m, mediasProperty.contains(m));
        orphaned(m);
        m.detach();
    }
}
