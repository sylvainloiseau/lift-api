package fr.cnrs.lacito.liftapi.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.change.ModelChange;
import javafx.util.Subscription;

/**
 * The change listeners of one dictionary, and the delivery of its events.
 *
 * <h2>Cost when nobody listens</h2>
 *
 * Every emission point of the model first asks {@link #anyObserved()}, a single read of a
 * counter shared by all dictionaries. While no dictionary has a listener - in particular
 * while a file is read, since a dictionary is only handed out once loaded - that is all
 * a change costs: no dictionary is looked up, no old value or position is computed, no
 * event is allocated.
 *
 * <h2>Rules</h2>
 *
 * <ul>
 * <li>Delivery is synchronous, on the thread that made the change, after it succeeded.</li>
 * <li>A listener must not change the dictionary while an event is delivered:
 * {@link #fire} refuses it with an {@link IllegalStateException}, which keeps the order of
 * the events the order of the changes.</li>
 * <li>An exception thrown by a listener does not stop the delivery to the others; the
 * first one is rethrown afterwards.</li>
 * </ul>
 *
 * <b>Not public API</b>: this package is not exported.
 */
public final class ChangeSupport {

    private static final AtomicInteger OBSERVED_DICTIONARIES = new AtomicInteger();
    private static final AtomicLong FIRED = new AtomicLong();

    private final List<Consumer<? super ModelChange>> listeners = new CopyOnWriteArrayList<>();
    private boolean delivering;

    /**
     * Whether any dictionary has a listener. The first test of every emission point.
     */
    public static boolean anyObserved() {
        return OBSERVED_DICTIONARIES.get() != 0;
    }

    /**
     * How many events have been delivered, by all dictionaries, since the start: lets a
     * test check that an operation reported nothing.
     */
    public static long firedCount() {
        return FIRED.get();
    }

    /**
     * Whether this dictionary has a listener.
     */
    public boolean isObserved() {
        return !listeners.isEmpty();
    }

    /**
     * Add a listener.
     *
     * @return the subscription that removes it
     */
    public Subscription addListener(Consumer<? super ModelChange> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        listeners.add(listener);
        if (listeners.size() == 1) {
            OBSERVED_DICTIONARIES.incrementAndGet();
        }
        boolean[] removed = {false};
        return () -> {
            if (removed[0]) {
                return;
            }
            removed[0] = true;
            // Remove this registration only, even if the same listener was added twice.
            for (int i = 0; i < listeners.size(); i++) {
                if (listeners.get(i) == listener) {
                    listeners.remove(i);
                    break;
                }
            }
            if (listeners.isEmpty()) {
                OBSERVED_DICTIONARIES.decrementAndGet();
            }
        };
    }

    /**
     * Deliver a change to the listeners.
     *
     * @throws IllegalStateException if called while a change is being delivered, that is
     *         if a listener changed the dictionary
     */
    public void fire(ModelChange change) {
        if (delivering) {
            throw new IllegalStateException(
                "A change listener must not modify the dictionary while a change is " +
                    "delivered: " + change
            );
        }
        FIRED.incrementAndGet();
        delivering = true;
        RuntimeException failure = null;
        try {
            for (Consumer<? super ModelChange> listener : listeners) {
                try {
                    listener.accept(change);
                } catch (RuntimeException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        } finally {
            delivering = false;
        }
        if (failure != null) {
            throw failure;
        }
    }
}
