package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public final class LiftPronunciation
    extends AbstractExtensibleWithField {

    protected final List<LiftMedia> medias = new ArrayList<>();
    @Getter @Setter private HasPronunciation parent;
    
    protected LiftPronunciation() {
    }

    public MultiText getProunciation() {
        return getMainMultiText();
    }

    public List<LiftMedia> getMedias() {
        return medias;
    }

    protected void addMedia(LiftMedia m) {
        medias.add(m);
        m.setParent(this);
    }

}
