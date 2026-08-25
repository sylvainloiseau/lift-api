package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasSense extends LiftObject permits LiftEntry, LiftSense {
    public void addSense(LiftSense sense);
    public List<LiftSense> getSenses();
    // use getSense() and operate on the list for these:
    // public void removeSense(LiftSense sense);
    // public LiftSense removeSenseAt(int index);
    // public void addSenseAt(LiftSense sense, int index);
}
