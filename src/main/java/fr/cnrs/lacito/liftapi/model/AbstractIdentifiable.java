package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

/**
 * Superclass of components that have Lift ID and Lift GUID.
 */
public abstract sealed class AbstractIdentifiable
    extends AbstractNotable
    implements Identifiable
    permits LiftEntry, LiftSense {

    protected Optional<String> id = Optional.empty();
    protected Optional<String> guid = Optional.empty();

    @Override
    public void setId(String id) {
        this.id = Optional.of(id);
    }

    @Override
    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

    public Optional<String> getId() {
        return id;
    }

    public Optional<String> getGuid() {
        return guid;
    }

}
