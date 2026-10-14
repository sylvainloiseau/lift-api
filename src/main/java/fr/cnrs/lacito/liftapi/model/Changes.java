package fr.cnrs.lacito.liftapi.model;

import java.util.Objects;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.ModelChange;
import fr.cnrs.lacito.liftapi.change.ModelProperty;
import fr.cnrs.lacito.liftapi.change.ValueChanged;
import fr.cnrs.lacito.liftapi.internal.ChangeSupport;

/**
 * Where the model reports its changes (see the {@code change} package).
 *
 * Every emission point calls {@link #observing(Object)} before it changes anything, and
 * only computes what the event needs (an old value, a position) when that returns a
 * dictionary. Its first test is {@link ChangeSupport#anyObserved()}, so while no
 * dictionary has a listener - while a file is read, for instance - a change costs one
 * field read more than before.
 */
final class Changes {

    private Changes() {}

    /**
     * The dictionary to report a change of {@code owner} to, or {@code null} when there
     * is nobody to report it to: no listener anywhere, {@code owner} detached, or its
     * dictionary not observed.
     */
    static LiftDictionary observing(Object owner) {
        if (!ChangeSupport.anyObserved()) {
            return null;
        }
        LiftDictionary d = dictionaryOf(owner);
        return d != null && d.getChangeSupport().isObserved() ? d : null;
    }

    /**
     * The dictionary {@code owner} belongs to, for reporting: an entry's subtree, or the
     * header's (unlike {@link AbstractLiftRoot#getOwningDictionary()}, which governs
     * registration and only knows entries).
     */
    static LiftDictionary dictionaryOf(Object owner) {
        return switch (owner) {
            case AbstractLiftRoot r -> r.eventDictionary();
            case MultiText m -> m.getParent() == null ? null : m.getParent().eventDictionary();
            case Form f -> f.getMultiText() == null ? null : dictionaryOf(f.getMultiText());
            case null, default -> null;
        };
    }

    static void fire(LiftDictionary dictionary, ModelChange change) {
        dictionary.getChangeSupport().fire(change);
    }

    /**
     * Report that a value of {@code owner} changed, if anybody listens and it did change.
     */
    static <T> void valueChanged(Object owner, ModelProperty<T> property, T oldValue, T newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        LiftDictionary d = observing(owner);
        if (d != null) {
            fire(d, new ValueChanged<>(d, owner, property, oldValue, newValue));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void valueChangedUnchecked(Object owner, ModelProperty<?> property, Object oldValue, Object newValue) {
        valueChanged(owner, (ModelProperty) property, oldValue, newValue);
    }
}
