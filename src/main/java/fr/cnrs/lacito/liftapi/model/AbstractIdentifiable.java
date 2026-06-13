package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import lombok.Getter;

/**
 * For Lift objects that have ID and GUID.
 */
public abstract sealed class AbstractIdentifiable
    extends AbstractNotable
    implements Identifiable
    permits LiftEntry, LiftSense {

    @Getter protected Optional<String> id = Optional.empty();
    @Getter protected Optional<String> guid = Optional.empty();

    @Override
    public void setId(String id) {
        this.id = Optional.of(id);
    }

    @Override
    public void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

}
