package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public final class LiftPronunciation extends AbstractExtensibleWithField {

    final ObservableList<LiftMedia> medias = FXCollections.observableArrayList();
    private ObservableList<LiftMedia> mediasView;

    private HasPronunciation parent;

    public HasPronunciation getParent() {
        return parent;
    }

    public LiftPronunciation() {}

    public MultiText getPronunciation() {
        return getMainMultiText();
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftPronunciation.
     * 
     * @param parent the new parent, or {@code null} when detaching this pronunciation
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(HasPronunciation parent) {
        this.parent = parent;
    }

    public ObservableList<LiftMedia> getMedias() {
        if (mediasView == null) mediasView = FXCollections.unmodifiableObservableList(medias);
        return mediasView;
    }

    public void addMedia(LiftMedia m) {
        addMedia(medias.size(), m);
    }

    /**
     * Add a media at a given position.
     *
     * @param index where to insert the media
     * @param m the media to add
     */
    public void addMedia(int index, LiftMedia m) {
        medias.add(index, m);
        m.setParent(this);
        adopted(m);
    }

    /** @deprecated use {@link #getMedias()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftMedia> mediasProperty() {
        return new ReadOnlyListWrapper<>(this, "medias", getMedias()).getReadOnlyProperty();
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
        requireChild(m, medias.contains(m));
        removeChild(m);
    }
}
