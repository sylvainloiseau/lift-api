package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import lombok.Getter;
import lombok.Setter;

public final class LiftPronunciation
    extends AbstractExtensibleWithField {

    protected final ListProperty<LiftMedia> mediasProperty =
            new SimpleListProperty<>(this, "medias", FXCollections.observableArrayList());
    @Getter @Setter private HasPronunciation parent;
    
    protected LiftPronunciation() {
    }

    public MultiText getProunciation() {
        return getMainMultiText();
    }

    public List<LiftMedia> getMedias() {
        return mediasProperty.get();
    }

    protected void addMedia(LiftMedia m) {
        mediasProperty.add(m);
        m.setParent(this);
    }

    public ListProperty<LiftMedia> mediasProperty() {
        return mediasProperty;
    }
}
