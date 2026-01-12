package fr.cnrs.lacito.liftapi.model;

public sealed interface HasGlosses permits LiftEtymology, LiftSense {
    public void addGloss(Form gloss);
    public MultiText getGloss();
}
