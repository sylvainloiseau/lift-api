package fr.cnrs.lacito.liftapi.model;

import java.util.List;

/**
 * Interface for LIFT components that can have {@link LiftPronunciation}.
 */
public sealed interface HasPronunciation extends LiftObject permits LiftEntry, LiftVariant {

    /**
     * Returns the pronunciations of this component.
     *
     * @return the pronunciations.
     */
    public List<LiftPronunciation> getPronunciations();

    /**
     * Adds a pronunciation to this component.
     *
     * @param pronounciation the pronunciation to add.
     */
    public void addPronunciation(LiftPronunciation pronounciation);

}
