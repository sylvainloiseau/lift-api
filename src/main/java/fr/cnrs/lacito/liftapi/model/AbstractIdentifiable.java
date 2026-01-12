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

    protected void setId(String id) {
        this.id = Optional.of(id);
    }

    protected void setGuid(String guid) {
        this.guid = Optional.of(guid);
    }

}
