package fr.cnrs.lacito.liftapi.internal;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the generation and allocation of unique UUIDs for Lift dictionary entries.
 *
 * <b>Not public API.</b> It lives in {@code fr.cnrs.lacito.liftapi.internal}, a package
 * {@code module-info.java} does not export, so no consumer of the library can reach it.
 *
 * <b>Not thread-safe.</b> Like the rest of the model (see the package documentation),
 * a dictionary is owned by a single thread - the JavaFX Application Thread when a UI
 * observes it. The check-then-pop in {@link #getUniqueUuid()} is the one place where
 * concurrent use would actually hand out a duplicate UUID.
 */
public class LiftDictionaryUUIDManager {

    private final static int DEFAULT_EXPECTED_NUMBER_OF_UUID = 2000;
    private final static int DEQUE_SIZE = 1000;

    private final Set<UUID> usedUuid;
    private final ArrayDeque<UUID> availableUuid = new ArrayDeque<>(DEQUE_SIZE);

    LiftDictionaryUUIDManager () {
        this(DEFAULT_EXPECTED_NUMBER_OF_UUID);
    }

    /**
    *
    * @param expectedNumberOfUuid the expected number of UUID that
    * will be needed by the dictionary. If an estimate is available,
    * if can speed-up the management of UUID
    */
    LiftDictionaryUUIDManager (int expectedNumberOfUuid) {
        if (expectedNumberOfUuid < 1) throw new IllegalArgumentException("expected number of uuid cannot be lower than 1");
        usedUuid = new HashSet<>(expectedNumberOfUuid);
        generateUniqueUuid();
    }

    private void generateUniqueUuid() {
        if (!availableUuid.isEmpty()) {
            throw new IllegalStateException("The list of available UUID should be empty");
        }
        Set<UUID> alreadyAddedInAvailableUuid = new HashSet<>(DEQUE_SIZE);
        int i = 0;
        while (i < DEQUE_SIZE) {
            UUID uuid = UUID.randomUUID();
            if (!usedUuid.contains(uuid) && !alreadyAddedInAvailableUuid.contains(uuid)) {
                alreadyAddedInAvailableUuid.add(uuid);
                availableUuid.addLast(uuid);
                i++;
            }
        }
    }

    UUID getUniqueUuid() {
        if (availableUuid.isEmpty()) {
            generateUniqueUuid();
        }
        UUID uuid = availableUuid.pop();
        usedUuid.add(uuid);
        return uuid;
    }
}
