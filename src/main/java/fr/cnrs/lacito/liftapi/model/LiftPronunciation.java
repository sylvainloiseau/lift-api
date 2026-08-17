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
    }

    public ListProperty<LiftMedia> mediasProperty() {
        return mediasProperty;
    }

    public static LiftPronunciation create() {
        return new LiftPronunciation();
    }
}
