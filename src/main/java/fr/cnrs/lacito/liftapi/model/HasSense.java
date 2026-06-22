package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasSense permits LiftEntry, LiftSense {
    public void addSense(LiftSense sense);
    public List<LiftSense> getSenses();
}
