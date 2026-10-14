package fr.cnrs.lacito.liftapi.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * The internal UUIDs: a per-dictionary prefix and a counter, laid out as version 8
 * UUIDs (see {@link LiftDictionaryUUIDManager}).
 */
class LiftDictionaryUUIDManagerTest {

    @Test
    void successiveUuidsAreDistinct() {
        LiftDictionaryUUIDManager manager = new LiftDictionaryUUIDManager();
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            assertTrue(seen.add(manager.getUniqueUuid()), "duplicate UUID after " + i);
        }
    }

    @Test
    void uuidsAreWellFormedVersion8() {
        UUID uuid = new LiftDictionaryUUIDManager().getUniqueUuid();
        assertEquals(8, uuid.version());
        assertEquals(2, uuid.variant(), "IETF variant");
    }

    @Test
    void twoDictionariesUseDifferentPrefixes() {
        UUID a = new LiftDictionaryUUIDManager().getUniqueUuid();
        UUID b = new LiftDictionaryUUIDManager().getUniqueUuid();
        assertNotEquals(a.getMostSignificantBits(), b.getMostSignificantBits());
        assertNotEquals(a, b);
    }
}
