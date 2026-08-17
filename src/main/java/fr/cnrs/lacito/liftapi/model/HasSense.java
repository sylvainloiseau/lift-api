package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasSense extends LiftObject permits LiftEntry, LiftSense {
    public void addSense(LiftSense sense);
    public List<LiftSense> getSenses();
}
