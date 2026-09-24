package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have glosses.
 */
public sealed interface HasGlosses permits LiftEtymology, LiftSense {

    /**
     * Adds a gloss to this component.
     *
     * @param gloss the gloss to add.
     */
    public void addGloss(Form gloss);

    /**
     * Returns the glosses of this component.
     *
     * @return the glosses.
     */
    public MultiText getGlosses();
}
