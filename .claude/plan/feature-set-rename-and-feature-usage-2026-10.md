# Renaming a feature set, and knowing whether a feature is used: plan (2026-10-18)

## Why

lift-patchbox is adding directives that manage the dictionary header (its plan
`header-declarations-2026-10.md`):

- `feature-set-rename`;
- `feature-delete` / `feature-set-delete`, refused while a feature is in use;
- `field-delete` / `trait-delete`, refused while a definition is in use.

Two things are missing from lift-api for that, and a third is package-private:

- **A. A feature set cannot be renamed.** `FeatureSet.id` is `final`. A feature can be
  renamed (`FeatureSet.changeFeatureId`), a feature set cannot.
- **B. Nothing says whether a feature is used, and `removeFeature` does not ask.**
  - `FeatureSet.removeFeature` removes a feature that notes, categories or traits
    still hold. They keep pointing at a feature that is no longer in its set, and the
    save writes a type or a value that no range declares.
  - `LiftHeader.removeFeatureSet` checks only that no definition takes the set as its
    range.
  - The definitions have the guard B lacks: `removeFieldOrTraitDefinition` refuses a
    definition in use (`isUsed(fd)`, package-private).
- **C.** `LiftHeader.isUsed(LiftFieldAndTraitDefinition)` is package-private, so an
  application has to repeat the scan.

The GUI (`../DictionaryEditor`) already counts the uses of a feature on its own
(`DictionaryQueries.countFeatureUsage`, used by `FeatureSetView` and `RangeView` to show
a count and to refuse a deletion). That count has three defects, which a model-side
answer fixes for every client:

- **Trait values.** It reads them as whitespace-separated tokens, but since D2 of
  `builder-only-construction-2026-10.md` multi-valued traits are comma-separated, and
  ids contain spaces (`"1.5 Plant"`).
- **Superordinates.** It ignores a feature that is the superordinate of another.
- **Header components.** It ignores the traits and annotations held by header
  components (`FeatureSet` and `Feature` are `AbstractExtensibleWithField`; FieldWorks
  puts traits on range elements), which `LiftDictionaryRegistry` does not index.

---

## A. `LiftHeader.renameFeatureSet(FeatureSet fs, String newId)`

### A.1 Findings

- `LiftHeader` keeps the sets in `featureSetsMap` (by id) and in `featureSets` (a list,
  in order of addition). `holds(fs)` compares `featureSetsMap.get(fs.getId()) == fs`, so
  the map must be re-keyed in the same step as the id changes.
- **Definitions** hold their range as the object (`resolvedRange`), and the writer emits
  `option-range` from `getResolvedFeatureSet().get().getId()`
  (`LiftWriterSession` l. 495). They follow a rename with no change.
- **Features, and every component typed by a feature**, hold `Feature` objects, which
  compare by identity. Nothing else refers to a set by its id.
- **External ranges** (`href`, `.lift-ranges`): the writer rewrites a ranges file only
  for the sets whose content was loaded (`isExternalContentLoaded()`,
  `LiftWriterSession` l. 389). A set with an `href` whose content was not loaded would
  be written into the header under its new id, while the untouched ranges file still
  has the old one, and the link breaks on reload. That rename must be refused.
- **Built-in sets** (`grammatical-info`, `note-type`, `translation-type`,
  `lexical-relation`, `reversal-type`, `annotation-type`, `etymology`):
  - their ids are the LIFT range names that the reader maps to the header managers
    (`getNoteTypeManager()`…);
  - renamed, they would be written under a name no reader recognises;
  - `removeFeatureSet` already refuses them, and the rename must too.
- **Change events.** `Feature.id` is a `TrackedProperty` reported as
  `ModelProperty.FEATURE_ID`, applied through `changeFeatureId`, and set last "so that
  the change is reported once the feature is under its new key". The set's id follows
  the same pattern.

### A.2 Design

- **`FeatureSet`:**
  - `final String id` → `private final TrackedProperty<String> id`, created with
    `ModelProperty.FEATURE_SET_ID`;
  - `getId()` unchanged;
  - new `public ReadOnlyObjectProperty<String> idProperty()`, for the GUI;
  - new package-private `void setId(String)`, called only by
    `LiftHeader.renameFeatureSet`;
  - the code reading the field `id` directly switches to `getId()`.
- **`LiftHeader.renameFeatureSet(FeatureSet fs, String newId)`**, public:
  - `IllegalArgumentException` if:
    - `fs` is not in this header;
    - it is built in;
    - `newId` is null or blank;
    - `newId` is already a set's id.
  - `IllegalStateException` if `fs` has an `href` and its external content was not
    loaded.
  - Then: `featureSetsMap.remove(old)`, `featureSetsMap.put(newId, fs)`, and
    `fs.setId(newId)` **last**, as `changeFeatureId` does. The position in
    `featureSets` is unchanged.
  - Every check happens before the first change, so a refusal leaves the header
    untouched (`ChangeRoundTripTest` requires it).
- **`ModelProperty.FEATURE_SET_ID`**, in the Header section next to `FEATURE_ID`:
  ```java
  /// The id of a feature set (a LIFT {@code range}); set through
  /// {@link LiftHeader#renameFeatureSet(FeatureSet, String)}.
  public static final ModelProperty<String> FEATURE_SET_ID = new ModelProperty<>(
      "featureSetId", (o, v) -> {
          FeatureSet s = (FeatureSet) o;
          s.parent.renameFeatureSet(s, v);   // or a package-level accessor
      });
  ```
  Add it to the list of all constants (l. ~201). `ValueChanged.revert()` then renames
  back through the public method.
- **Javadoc:**
  - `FeatureSet` class: the id can change, through the header;
  - `getFeatureSet(String)` / `hasFeatureSet(String)`: an id is current, not
    permanent;
  - `LiftHeader`: list the built-in ids once, as the reason both
    `removeFeatureSet` and `renameFeatureSet` refuse them.

### A.3 Tests

- **`RegressionTest` or `HeaderTextsTest`** (where feature-set tests live):
  - after a rename, `getFeatureSet(new) == fs`, `hasFeatureSet(old)` is false, and
    `holds(fs)` still holds;
  - a definition whose range is the set reports the new id;
  - each refusal (built-in, duplicate id, blank id, not in this header, external
    content not loaded) leaves the header unchanged.
- **Round trip:** a small dictionary with an option trait definition whose range is
  renamed. Load → rename → save → load: `option-range` names the new id, and the trait
  values still resolve. A set with an `href` whose content was loaded: its
  `.lift-ranges` file is rewritten with the new id.
- **`change/ChangeEventsTest`:** a rename reports exactly one `ValueChanged` on
  `FEATURE_SET_ID` (old → new), and nothing when refused.
- **`change/ChangeRoundTripTest`**, op `"feature set"`: add a rename case
  (`renameFeatureSet(pick, "range-" + fresh())`), refused for built-ins as the harness
  allows. `ModelSnapshot` already keys the sets by `getId()`, so a rename that is not
  reverted is caught.

---

## B. Feature usage: `LiftHeader.countUses(Feature)`, `isUsed(Feature)`, and the guards

### B.1 What uses a feature

A feature `f` is used by:

1. **The `type` of a typed component.** The six `HasType` classes are `LiftNote`,
   `LiftRelation`, `LiftAnnotation`, `LiftEtymology`, `LiftReversal` and
   `LiftTranslation`. The check is `getType() == f`.
2. **A category:** `GrammaticalInfo.getGramInfoValue() == f`.
3. **An option trait:** `LiftTrait.getTypedValue()` is `f`, or a `Set` / `List`
   containing it. Compare by identity on the typed value, never by parsing the string
   value.
4. **Another feature**, of any set: its `getSuperordinateFeature()` is `f`.

Places to look:

- **The registry's lists** for 1–3: `getNotes()`, `getRelations()`,
  `getAnnotations()` (including annotations on texts and forms, which
  `DictionaryMutator.childrenOf` registers), `getEtymologies()`, `getReversals()`,
  `getTranslations()`, `getGrammaticalInfos()`, `getTraits()`.
- **The header**, which the registry does not index: the traits and annotations held
  by every `FeatureSet` and every `Feature` (and the annotations of their texts), for
  1 and 3; every feature of every set, for 4.

### B.2 Design

- **`public long countUses(Feature f)`** on `LiftHeader`, beside `isUsed(fd)`:
  - the number of uses listed in B.1, each component counted once;
  - 0 when the header belongs to no dictionary, as `isUsed(fd)` does (a set being
    built has no uses).
  - The GUI shows this count per feature.
- **`public boolean isUsed(Feature f)`:** `countUses(f) > 0`, but stopping at the
  first use (`anyMatch` per source).
- **Cost:** one linear pass over the typed components (≈ 10⁴ on the reference corpus)
  per call. That is fine for a deletion or a dialog. The GUI's per-row count makes
  it O(features × components); if that shows, a usage index can be added later
  (§D, open question).
- **Guards**, refused before anything changes:
  - `FeatureSet.removeFeature(id)`: `IllegalStateException` if
    `headerDictionary() != null && header.isUsed(feature)`. A set not yet in a header
    keeps removing freely, as now.
  - `LiftHeader.removeFeatureSet(fs)`: also `IllegalStateException` if one of its
    features is used. This is in addition to the existing range check, and after the
    built-in check.
  - Undo stays consistent. Events are reverted in reverse order, so when a
    `FeatureAdded` is reverted by `removeFeature`, the uses recorded after it have
    already been reverted. `FeatureRemoved.revert` adds, so it is unaffected.
- **Header traversal:** a private helper in `LiftHeader`,
  `headerTraitsAndAnnotations()`, collecting the traits and annotations (recursively:
  annotations of a trait, annotations of texts) of the sets and features. It is used
  by `countUses`. It is not an index and allocates nothing when nothing is used.

### B.3 Definitions (item C)

- Make `isUsed(LiftFieldAndTraitDefinition)` **public**, with a javadoc stating it
  covers the fields and traits of the dictionary.
- Add `public long countUses(LiftFieldAndTraitDefinition fd)` for symmetry with
  features. The GUI's `countFieldOrTraitUsage` compares names; this compares the
  definition object, which is what `removeFieldOrTraitDefinition` refuses on.
- Also cover the traits held by header components, as B.1 does. A trait on a
  range element uses its definition too, and today such a definition could be removed
  under it.

### B.4 Tests

- **`RegressionTest`:**
  - one case per use of B.1: note type, translation type, relation type, annotation
    type (on a component and on a form), etymology, reversal, category, option trait
    (single, collection, sequence, with an id containing a space), superordinate, and
    a trait on a range element. For each, `isUsed` is true, `countUses` is right, and
    `removeFeature` is refused and leaves the set unchanged;
  - `removeFeatureSet` refused when one of its features is used;
  - `isUsed(fd)` is true for a definition used only by a trait on a feature, and
    `removeFieldOrTraitDefinition` refuses it.
  - Existing `featuresPropertyTracksTheFeatureMap` (l. 377) removes an unused feature:
    unchanged.
- **`HeaderTextsTest`** (l. 132, 155): the sets and features removed there are unused.
  Check they still pass, and add none.
- **`change/ChangeRoundTripTest`:** op `"feature"` case 1 may now pick a used
  feature. The harness accepts a refusal provided it changes and reports nothing,
  which the guard guarantees. Optionally weight the pick toward unused features, so
  that removal is still exercised.
- **`DictionaryCensusTest` / corpus:** on `20240828Lift.lift`, every feature of
  `grammatical-info` that a sense uses is reported used, and a fresh feature is not.

---

## C. Documentation

- **`CLAUDE.md`, Core design:**
  - next to "Anything keyed by a type uses the `Feature` object…", add: "a feature set
    is renamed only through `LiftHeader.renameFeatureSet`, which re-keys the header;
    built-in sets and sets whose external ranges were not loaded cannot be renamed";
  - add: "a feature or a feature set in use cannot be removed
    (`LiftHeader.isUsed(Feature)`), as a definition in use cannot".
- **`model/package-info.java`**, if it describes the header's contracts: the same two
  sentences.
- **`TODO.md`:** remove any item this closes.
- **For `../DictionaryEditor`** (not in this module, noted for its maintainer):
  - replace `DictionaryQueries.countFeatureUsage` with `LiftHeader.countUses(Feature)`,
    which fixes its three defects;
  - replace `countFieldOrTraitUsage` with `countUses(LiftFieldAndTraitDefinition)`;
  - `FeatureSetView` / `RangeView` keep their pre-check, and may also catch the new
    `IllegalStateException`;
  - a rename action for sets can call `renameFeatureSet`.

## D. Open questions

- **A usage index.** If the GUI's per-row count is slow on large dictionaries, keep a
  per-feature use counter in `DictionaryRegisters`, updated on registration and on
  `setType`/value changes. Out of scope until measured.
- **A shared `.lift-ranges` file.** A ranges file loaded and rewritten after a rename
  may be shared with other projects (FieldWorks writes one per project, so this is
  unlikely). It is noted in the `renameFeatureSet` javadoc, not guarded.

## E. Order of work

1. B (usage and guards) with its tests. It changes behaviour, so it comes first and
   gets its own commit.
2. B.3 (definition usage made public, header traits included) with its tests.
3. A (rename) with its tests and the `ModelProperty`.
4. Documentation; `mvn clean test`, `ChangeRoundTripTest` included; `mvn install` so
   that lift-patchbox can build against it.

## F. Results (2026-10-18)

Implemented, uncommitted. 207 tests pass (192 before), `ChangeRoundTripTest` included;
installed with `mvn install`. lift-patchbox's 440 tests pass against it.

- **A.** `FeatureSet.id` is a `TrackedProperty` (`ModelProperty.FEATURE_SET_ID`), with
  `idProperty()` and a package-private `setId`; `LiftHeader.renameFeatureSet` as
  designed. Tests: `RegressionTest` (in place, refusals, save/reload with the
  `option-range` and trait values, external ranges file rewritten),
  `ChangeEventsTest.aFeatureSetRenameIsReportedOnceAndReverted`, and a rename case in
  `ChangeRoundTripTest` (whose added sets now get an `href` half the time, so that
  renames also succeed).
- **B.** `LiftHeader.isUsed(Feature)` / `countUses(Feature)`, the guards in
  `FeatureSet.removeFeature` and `LiftHeader.removeFeatureSet`, and the header walk
  (`heldByTheHeader`). Added to the design: **what a set holds itself does not count
  when the whole set is removed** (its hierarchy, the traits of its elements), or a
  hierarchical set such as the semantic domains could never be removed. Tests:
  `RegressionTest` (one per use, with an id containing a space), the corpus check
  `DictionaryCensusTest.thePartsOfSpeechOfTheCorpusAreReportedUsed`, and
  `ChangeRoundTripTest` now mostly removes unused features.
- **B.3.** `isUsed(LiftFieldAndTraitDefinition)` is public, with `countUses`, and
  counts the fields and traits held by the header.

**Deviation: the XML reader.** Counting the header's traits revealed a latent defect.
The traits of a range element, read before the header's `<fields>`, create their
definition on the fly. When `<fields>` then declared it, the decoder removed it and
built a new one, so those traits held a definition the header no longer had. Now that
removal is refused, a file saved from the Tuwuli dictionary (FieldWorks'
`absten-feature-value`) could not be read back. `LiftXmlDecoder.fieldDefinition` now
applies such a declaration **in place** (`redeclared`):

- the attributes it gives;
- its label and description, replacing the earlier ones;
- its `option-range`.

It does so only when the earlier definition is in use, and otherwise removes and
rebuilds as before. Test:
`RegressionTest.aDefinitionDeclaredAfterTheTraitsOfARangeElementIsAppliedToThem`.

**Not done here:**
- `TODO.md` was not checked: a permission rule blocks reading it.
- The `../DictionaryEditor` changes of §C are left to its maintainer: replace
  `DictionaryQueries.countFeatureUsage` / `countFieldOrTraitUsage` with
  `LiftHeader.countUses`.
