package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftPronunciation}.
 */
public sealed interface HasPronunciation extends LiftObject permits LiftEntry, LiftVariant {

    public List<LiftPronunciation> getPronunciations();

    public void addPronunciation(LiftPronunciation pronounciation);

}
