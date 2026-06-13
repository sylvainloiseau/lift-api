package fr.cnrs.lacito.liftapi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class LiftDictionaryUUIDManager {
    private final Set<UUID> usedUuid = new HashSet<>(2000);
    private final Set<UUID> availableUuid = new HashSet<>(2000);
    private Iterator<UUID> uuidIterator;

    public LiftDictionaryUUIDManager () {
        generateUniqueUuid();
    }

    private void generateUniqueUuid() {
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUID.randomUUID();
            if (!usedUuid.contains(uuid) && !availableUuid.contains(uuid)) {
                availableUuid.add(uuid);
            }
        }
        uuidIterator = availableUuid.iterator();
    }

    protected UUID getUniqueUuid() {
        if (!uuidIterator.hasNext()) {
            generateUniqueUuid();
        }
        UUID uuid = uuidIterator.next();
        //availableUuid.remove(uuid);
        usedUuid.add(uuid);
        return uuid;
    }    
}
