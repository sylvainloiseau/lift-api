package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * Interface for LIFT components that can have a reference towards other lift component.
 */
public sealed interface HasRefId permits LiftVariant, LiftRelation {
    public Optional<String> getRefId();
    public AbstractIdentifiable getRefObject();
    public void setRefObject(AbstractIdentifiable refObject);
}
