# Cheaper internal UUIDs: plan (2026-10-14)

## Why

Registering a component costs a UUID, and loading registers every component and every
MultiText: 25,254 UUIDs for `20240828Lift.lift` (14,165 components, 11,089 MultiTexts).
A profile of the load (JFR, 200 loads after warm-up) shows that registration is about
half of the load time (9 of 17 ms), and that `DictionaryRegisters.newUUID()` alone is
**~21% of the whole load**:

| Inclusive share of the load | |
|---|---|
| `LiftDictionaryUUIDManager.getUniqueUuid` | 20.6% |
| ... of which `generateUniqueUuid` (the batch refill) | 15.4% |
| ... of which `UUID.randomUUID` → `SecureRandom` (NativePRNG) | 6.2% |
| `HashMap.resize` (self time; the UUID sets are one of its callers) | 9.6% |

The UUIDs are internal keys. Nothing needs them to be unpredictable, so paying for a
cryptographic generator, and for a set that guarantees uniqueness after the fact, buys
nothing.

Expected gain: 15–20% of the load time. To be confirmed with `LoadingCostTest`.

---

## 1. Findings

**F1. A cryptographic generator per registration.** `LiftDictionaryUUIDManager` fills a
deque of 1,000 UUIDs from `UUID.randomUUID()`, which draws 16 bytes from `SecureRandom`
for each.

**F2. Two sets enforce a uniqueness the generator could guarantee itself.**
- `usedUuid` (a `HashSet`, presized for 2,000, so it is resized several times during a
  load) records every UUID ever handed out.
- `alreadyAddedInAvailableUuid`, a second set built for each batch, guards against
  duplicates inside the batch.
- Both guard against a random 122-bit collision, which does not happen in practice.

**F3. `usedUuid` never shrinks.** `unregister` does not remove the UUID. A long editing
session, with every undo/redo of an addition re-registering a subtree under fresh
UUIDs, grows the set without bound. Small, but a leak.

**F4. The UUID is also used as the default LIFT id.** `DictionaryMutator.register()` gives
an entry or a sense without an id `uuid.toString()` as its LIFT id. That value **is
written to the file**, and must stay globally unique: two files, or two sessions on the
same file, must not generate the same id. This is the one place where the randomness
of the current UUIDs matters.
- On load it never fires on the corpus: every entry and sense has an id.
- It fires on every entry and sense created in the editor without an explicit id.

**F5. Nothing depends on the value of the internal UUIDs.**
- No use of `getUUID()` or UUID values in DictionaryEditor, in the `change` package or in
  the `xml` package (searched).
- `LiftDictionaryRegistry.getEntriesById()` exposes them as opaque keys.
- No test compares a LIFT id with a UUID.
- `DictionaryMutator.isRegisteredHere()` compares the indexed node by **identity**
  (`nodesById(node).get(uuid) == node`). A UUID that happened to exist in two dictionaries
  would still be detected as "registered in another dictionary". Cross-dictionary
  uniqueness is therefore not a correctness requirement, only a convenience.

---

## 2. Design

**A per-dictionary prefix and a counter, laid out as an RFC 9562 version-8 ("custom")
UUID.**

```java
// most significant 64 bits: random once per dictionary, version nibble = 8
prefix = (seed & ~0xF000L) | 0x8000L;
// least significant 64 bits: variant '10' + a 62-bit counter
UUID next() {
    return new UUID(prefix, (++counter & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L);
}
```

- **Unique within a dictionary by construction:** the counter never repeats. 2^62 values
  will not run out. No set is needed, and F3 disappears with it.
- **Distinct between dictionaries in practice:** the prefix carries 60 random bits. It is
  drawn once per dictionary, from `SecureRandom` or `ThreadLocalRandom`; the cost is
  irrelevant at that frequency. Correctness does not rely on it (F5).
- **Still a well-formed `java.util.UUID`:** version 8 and the IETF variant say "not
  random, application-defined". So nobody is misled into treating them as random
  identifiers, and `getUUID()`'s type does not change.
- **Cost:** one `new UUID(long, long)` and an increment. No hashing, no allocation
  besides the UUID itself.

**The default LIFT id becomes independent of the internal UUID (F4).** `register()` gives
an entry or a sense without an id `UUID.randomUUID().toString()`, a random version-4
UUID as today. It is called only for components without an id, which is rare on load.
The format of the generated ids does not change, so files are unaffected.

Rejected alternatives:
- **`ThreadLocalRandom` 128-bit values:** cheaper than `SecureRandom`, but they still need a
  set to be guaranteed unique, so they keep F2 and F3.
- **A plain counter (`new UUID(0, n)`):** two dictionaries would hand out the same
  UUIDs, which makes debugging output ambiguous when two dictionaries are open.

---

## 3. Work plan

1. **`internal/LiftDictionaryUUIDManager`**: replace the deque, both sets and
   `generateUniqueUuid()` with the prefix + counter of section 2.
   - Keep the class name and `getUniqueUuid()`, which limits the change to one file.
   - Drop the `expectedNumberOfUuid` constructor, now meaningless.
   - Rewrite the javadoc: what the UUIDs are for (internal keys, never written to a file),
     why they are not random, why uniqueness is per dictionary and why that suffices
     (identity check in `isRegisteredHere`).
   - Keep the "not thread-safe" note: the counter increment is the one racy place now.
2. **`internal/DictionaryMutator.register()`**: generate the default LIFT id of entries and
   senses with `UUID.randomUUID().toString()` instead of the internal UUID. Javadoc
   (the "giving it a LIFT id" bullet): say that the LIFT id is written to the file and
   therefore random, while the internal UUID is not.
3. **`internal/DictionaryRegisters.newUUID()`**: javadoc only. It now points at the
   generator's contract.
4. **Tests**:
   - New `internal`-level unit test for the generator. It must sit in the same module;
     a test in package `fr.cnrs.lacito.liftapi.internal` works as for other internal
     tests. It checks:
     - 100,000 successive UUIDs are distinct;
     - `version() == 8` and `variant() == 2`;
     - two managers have different prefixes.
   - `RegressionTest`: an entry created without an id gets a LIFT id that is a version-4
     UUID, different from its `getUUID()`.
   - `RegressionTest`: save a dictionary, reload it, create an entry without an id.
     There must be no `DuplicateIdException`, and the new id must differ from every
     saved one. This is the case a counter-based LIFT id would break.
   - The existing suite (`AttachmentTest`, `DeletionTest`, `ChangeRoundTripTest`, census,
     round trip) must pass unchanged.
5. **Measure**: `LoadingCostTest` before and after (median time and retained heap), plus
   the JFR profile. Record the figures in section 5 of this file.
6. **Docs**: one line in `CLAUDE.md`'s package map entry for `internal` ("UUID manager:
   per-dictionary counter, not random").

---

## 4. Risks

- **Persisted data:** none. Internal UUIDs are never written. Default LIFT ids stay
  random UUIDs with the same textual format (step 2).
- **Consumers:** none found (F5). A consumer keying something across dictionaries by
  `getUUID()` would still get distinct keys in practice (60-bit random prefix).
- **Ordering:** the UUIDs now sort in registration order. Harmless, and nothing must
  start relying on it. The javadoc says so.

## 5. Results (2026-10-14)

Implemented as planned, with one simplification: the `expectedNumberOfUuid` constructor
was removed rather than kept unused, since nothing called it.

Timing of `20240828Lift.lift` (1,927 entries), same machine, JDK 26. "Long" runs mean 300
warm-up loads and then 200 timed loads, three runs each. `LoadingCostTest` (10 warm-up,
25 timed) is noisier and is shown for reference:

| | Before | After |
|---|---|---|
| Long runs, median | 17.5 / 17.7 / 17.1 ms | 14.3 / 12.9 / 12.7 ms |
| `LoadingCostTest`, median | 22 / 22 / 18 ms | 17 / 16 ms |
| Retained heap (`LoadingCostTest`) | 26.7–26.8 MB | 25.6 MB |

About **−24%** load time, more than the 15–20% expected: the `HashMap.resize` share from the
UUID sets went away too. The heap saving is the `usedUuid` set (F2, F3).

Tests:
- `internal/LiftDictionaryUUIDManagerTest`: distinctness over 100,000 UUIDs, version 8
  with the IETF variant, different prefixes for two managers.
- `RegressionTest.aDefaultLiftIdIsARandomUuidDistinctFromTheInternalOne`.
- `RegressionTest.idsGeneratedAfterAReloadDoNotCollideWithSavedOnes`.
- Full suite: 164 tests, all passing.
