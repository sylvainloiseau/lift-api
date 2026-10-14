package fr.cnrs.lacito.liftapi.internal;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Hands out the UUIDs under which a dictionary indexes its components and MultiTexts.
 *
 * These UUIDs are internal keys: they are never written to a file (the default LIFT id
 * of an entry or a sense is a separate, random UUID, see
 * {@code DictionaryMutator.register}), and nothing needs them to be unpredictable. They
 * are therefore not random. Each one is made of:
 *
 * <ul>
 * <li>a 64-bit prefix drawn once per dictionary, in the most significant bits;</li>
 * <li>a counter, in the least significant bits.</li>
 * </ul>
 *
 * laid out as an RFC 9562 version 8 ("custom") UUID, so that nobody mistakes them for
 * random ones.
 *
 * <b>Why not {@link UUID#randomUUID()}.</b> Loading registers every component and every
 * MultiText - about 25,000 UUIDs for a 2,000-entry dictionary - and drawing each from
 * {@code SecureRandom}, then checking it against a set of every UUID handed out so far,
 * was about a fifth of the loading time. The counter is unique within a dictionary by
 * construction, so it needs neither the randomness nor the set (which also never shrank
 * when a component was unregistered).
 *
 * <b>Uniqueness is per dictionary, and that is enough.</b> The random prefix makes two
 * dictionaries hand out different UUIDs in practice, but correctness does not depend on
 * it: {@code DictionaryMutator.isRegisteredHere} compares the indexed component by
 * identity, so a component carrying a UUID that happens to exist in another dictionary
 * is still recognised as foreign.
 *
 * The UUIDs of a dictionary sort in the order they were handed out. Nothing relies on
 * it, and nothing should.
 *
 * <b>Not public API.</b> It lives in {@code fr.cnrs.lacito.liftapi.internal}, a package
 * {@code module-info.java} does not export, so no consumer of the library can reach it.
 *
 * <b>Not thread-safe.</b> Like the rest of the model (see the package documentation),
 * a dictionary is owned by a single thread - the JavaFX Application Thread when a UI
 * observes it. The counter increment in {@link #getUniqueUuid()} is the one place where
 * concurrent use would actually hand out a duplicate UUID.
 */
public class LiftDictionaryUUIDManager {

    /** Bits 12-15 of the most significant half: the version nibble. */
    private static final long VERSION_MASK = 0xF000L;
    private static final long VERSION_8 = 0x8000L;

    /** The two top bits of the least significant half: the variant. */
    private static final long COUNTER_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long IETF_VARIANT = 0x8000_0000_0000_0000L;

    /**
     * Drawn once per dictionary, so its cost does not matter; a shared instance would
     * not be cheaper in any way that counts.
     */
    private static final SecureRandom SEED = new SecureRandom();

    private final long prefix;
    private long counter = 0;

    LiftDictionaryUUIDManager() {
        this.prefix = (SEED.nextLong() & ~VERSION_MASK) | VERSION_8;
    }

    UUID getUniqueUuid() {
        // 2^62 values per dictionary: the counter cannot wrap around in practice.
        counter++;
        return new UUID(prefix, (counter & COUNTER_MASK) | IETF_VARIANT);
    }
}
