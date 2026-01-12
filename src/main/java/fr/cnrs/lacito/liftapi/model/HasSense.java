package fr.cnrs.lacito.liftapi.model;

public sealed interface HasSense permits LiftEntry, LiftSense {
    public void addSense(LiftSense sense);
}
