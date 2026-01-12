package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasPronunciation permits LiftEntry, LiftVariant {
    
    public List<LiftPronunciation> getPronunciations();

    public void addPronunciation(LiftPronunciation pronounciation);

}
