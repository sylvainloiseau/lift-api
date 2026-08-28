package fr.cnrs.lacito.liftapi.model;

/**
 * Interface for LIFT components that can have glosses.
 */
public sealed interface HasGlosses permits LiftEtymology, LiftSense {
    public void addGloss(Form gloss);
    public MultiText getGloss();
}
