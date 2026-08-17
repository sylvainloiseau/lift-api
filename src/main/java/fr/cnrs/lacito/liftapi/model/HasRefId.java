package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * For LIFT objects that have a reference towards others LIFT object thanks to
 * an idRef.
 */
public sealed interface HasRefId permits LiftVariant, LiftRelation {
    public Optional<String> getRefId();
    public AbstractIdentifiable getRefObject();
    public void setRefObject(AbstractIdentifiable refObject);
}
