package fr.cnrs.lacito.liftapi.model;

import java.util.List;

public sealed interface HasReversal permits LiftSense, LiftReversal {
    public void addReversal(LiftReversal reversal);
    public List<LiftReversal> getReversals();
}
