# Trait and field specification: definition, range, or undeclared name (2026-10)

**Status: implemented** (2026-10-21) in lift-api and DictionaryEditor, with the
recommended answers to Q1–Q4 (Q5 not done). Differences from the plan: §4 also
rebinds when `setKind` widens a definition (FIELD → UNKNOWN makes it fit traits);
`getTypeStr()` returns a `String`; `getType()` and `setDefinitionType` are removed;
`LiftTrait.getValue()` returns `""` for an option or date not set (it threw).
**Option A chosen** (2026-10-21). The public API may change; DictionaryEditor will be
updated with it. Open questions are in §9, with a recommended answer each; the plan is
written with those answers.

Companion to `trait-type-resolution-2026-10.md` (the "implicit definition" plan, kept as it
was, superseded by this one). The options weighed are recalled in §11.

## 1. Problem

The name of a trait (`<trait name="x">`) and of a field (`<field type="x">`, 0.13
`<field tag>`) is always turned into a `LiftFieldAndTraitDefinition`: `TraitBuilder` and
`FieldBuilder` call `LiftHeader.getOrCreateTraitsDefinitions` / `getOrCreateFieldDefinitions`,
which create one when the header declares none.

- **A trait whose name is a range id gets a STRING definition.** On the corpus (v0.13,
  `20240828Lift.lift`), 9 trait names are range ids: morph-type 3301 traits,
  semantic-domain-ddp4 854, Noun-infl-class 121, inflection-feature 117, Verb-slot 107,
  Noun-slot 28, from-part-of-speech 10, status 2… Their values are strings, not features:
  - `countUses(Feature)` / `isUsed(Feature)` do not see them;
  - `removeFeature` and `removeFeatureSet` accept a feature they use;
  - `changeFeatureId` does not rename their values;
  - `LiftDictionaryCounterManager` does not count them per range.
- **The header gains what the file never declared**: a load-save adds 14 `<field>` (12
  trait names, plus the fields `note` and `summary`).

LIFT (p. 13): *"Where no range is linked the name is informal or resolved by its use in a
field-definition."* A trait name therefore refers to one of three things, and the model
should say which.

## 2. The model

### 2.1 Types (package `model`)

```java
/**
 * What the name of a trait refers to: a definition declared in the header, a range
 * (the name is the id of a feature set: the value is one of its features), or nothing
 * (the name is informal: the value is a string).
 */
public sealed interface TraitSpecification
    permits LiftFieldAndTraitDefinition, RangeReference, UndeclaredName {

    /** The name written on the traits: the definition's name, the range id, the name. */
    String getName();

    /** The data model of the values: the definition's, FEATURE, STRING. */
    LiftFieldAndTraitDefinitionDataModel getDataModel();

    /** The feature set the values come from: the option-range, the range itself, empty. */
    Optional<FeatureSet> getResolvedFeatureSet();
}

/** What the name of a field refers to: a definition, or nothing. */
public sealed interface FieldSpecification
    permits LiftFieldAndTraitDefinition, UndeclaredName {
    String getName();
}

/** A trait whose name is the id of a feature set of the header. */
public record RangeReference(FeatureSet range) implements TraitSpecification {
    public String getName() { return range.getId(); }      // follows renameFeatureSet
    public LiftFieldAndTraitDefinitionDataModel getDataModel() { return FEATURE; }
    public Optional<FeatureSet> getResolvedFeatureSet() { return Optional.of(range); }
}

/** A field or trait name declared nowhere: LIFT's "informal" name. */
public record UndeclaredName(String getName) implements TraitSpecification, FieldSpecification {
    public LiftFieldAndTraitDefinitionDataModel getDataModel() { return STRING; }
    public Optional<FeatureSet> getResolvedFeatureSet() { return Optional.empty(); }
}
```

`LiftFieldAndTraitDefinition implements TraitSpecification, FieldSpecification`.

Equality: `RangeReference` is equal for the same `FeatureSet` object (FeatureSet does not
override `equals`, so it compares by identity) and `UndeclaredName` for the same name.
That is the right meaning for both. The definition keeps identity.

**Range case: a record, not `FeatureSet` itself (Q1).**
- `getDataModel()` and `getResolvedFeatureSet()` would look odd on `FeatureSet`'s own
  API, which the GUI uses everywhere for other purposes.
- The record follows renames, since its name is computed.
- One small record per trait costs nothing. `LiftHeader` may cache one per set
  (`rangeReference(fs)`) so that all the `morph-type` traits share one; not needed for
  correctness.

**`getDataModel()` no longer returns an `Optional` (I1, improvement).** It is always
present today (`Optional.of(definitionType)`), and ~25 call sites write
`.getDataModel().get()`. The API may change, so the interface returns the value, and
`LiftFieldAndTraitDefinition.getDataModel()` too. `getType()` / `getTypeStr()` (aliases)
can go at the same time.

### 2.2 `LiftTrait` and `LiftField`

- `LiftTrait`:
  - holds a `TraitSpecification`, which still cannot change (§5 says how a trait is
    rebound);
  - constructors take a `TraitSpecification`, and switch on
    `spec.getDataModel()` as now;
  - `getSpecification()` returns `TraitSpecification`;
  - new `getName()` shortcut, since ~25 call sites write
    `getSpecification().getName()`;
  - `parse(...)` takes the specification: a `RangeReference` parses like a FEATURE
    definition (lenient creates the feature in the range, strict refuses an unknown id).
- `LiftField`:
  - holds a `FieldSpecification`, which can change (§5: a field's storage does not
    depend on it);
  - `specificationProperty()` becomes a tracked `ObjectProperty`, read-only from
    outside, reported as `ModelProperty.FIELD_SPECIFICATION`;
  - new `getName()`.
  - The parent's `fields` map stays keyed by the name, which a rebinding does not
    change.

### 2.3 The invariant

> **Every trait's specification is what `LiftHeader.resolveTraitSpecification(trait.getName())`
> returns now, and every field's is what `resolveFieldSpecification(field.getName())` returns.**

That is what makes the in-memory model equal to what a save-and-reload gives: the writer
writes names, the reader resolves them. Every header operation below either keeps it, or
rebinds the traits and fields it affects, or is refused.

## 3. Resolution

```java
// LiftHeader
public TraitSpecification resolveTraitSpecification(String name)
public FieldSpecification resolveFieldSpecification(String name)
```

Trait:
1. a definition of that name **whose kind is not FIELD** → the definition;
2. else a feature set of that id → `RangeReference(set)`;
3. else → `UndeclaredName(name)`.

Field:
1. a definition of that name whose kind is not TRAIT → the definition;
2. else → `UndeclaredName(name)`. A field's value is a text, never a feature.

Notes:
- **Definition before range.** A declaration says what the values are: possibly another
  range (`option-range`), or a collection (`option-collection`). The corpus has no name
  that is both a range and a definition (checked).
- **A definition of the other kind is skipped (Q3)** rather than throwing as
  `getOrCreateTraitsDefinitions` does today: today, a `multitext` field definition
  named like a trait makes the load fail. The reader logs a warning. The rule depends
  only on the header, so reloading gives the same answer.
- **Kind UNKNOWN** (declared with no `type`) fits both, as now: data model STRING.
- **Built-in feature sets count as ranges.** A trait named `note-type` gets the
  `note-type` set. That's harmless: the corpus has none, and it is what the name says.

`TraitBuilder` and `FieldBuilder` use these. `getOrCreateTraitsDefinitions` /
`getOrCreateFieldDefinitions` stay as **explicit declarations**. I suggest renaming them
`declareTraitDefinition` / `declareFieldDefinition` so that no one takes them for the
lookup (I2). Through §4 a declaration rebinds the traits of that name, so declaring a
range id as a STRING definition is allowed and consistent with a reload.

## 4. Header operations

| Operation | Today | With A |
|---|---|---|
| `addFieldOrTraitDefinition(d)` (and `createXDefinition`, `declareX`, `FieldDefinitionBuilder.build`) | throws if the name exists, including an invented one | throws only for a real duplicate; **rebinds** the traits (`RangeReference`/`UndeclaredName`) and fields (`UndeclaredName`) of that name to `d`, if `d`'s kind fits |
| `removeFieldOrTraitDefinition(d)` | refused while used | unchanged (by the invariant, used ⇔ a trait or field has the name) |
| `addFeatureSet(fs)` | — | **rebinds** the `UndeclaredName` traits named `fs.getId()` to `RangeReference(fs)`, unless a definition has that name |
| `removeFeatureSet(fs)` | refused if a definition has it as range, or a feature is used | also refused while a `RangeReference` trait points to it, even one with no value (new `isUsed(FeatureSet)` / `countUses(FeatureSet)`) |
| `renameFeatureSet(fs, id)` | re-keys | `RangeReference` traits follow (written under `id`). **Refused** if a definition named `id` would take them on reload (it fits traits). The `UndeclaredName` traits named `id` are **rebound** to the set: on reload they would be |
| `FeatureSet.removeFeature` / `changeFeatureId` | blind to range-keyed traits | see them: they hold `Feature` objects |
| `definition.setKind(FIELD)` while traits use it (or `TRAIT` while fields do) | allowed | **refused**: the resolution would skip it (Q3) |
| `definition.setDataModel(...)` while used | refused | unchanged |

Rebinding is all or nothing. A value that does not parse for the new specification
(`"abc"` for an `integer` definition) makes the whole operation throw before anything
changes. Values are parsed **leniently**, as the reader does: a feature id the range
lacks is created (reported as `FeatureAdded`), so the result is what a reload would give.

## 5. Rebinding, and undo

**Traits: replaced, not changed in place (Q2).** The value is stored in a property whose
kind depends on the data model (string, feature, set…). Changing a trait's specification
would swap those properties under any GUI binding, and break the guarantee that a
trait's value property is the same object for its whole life, which `LiftTrait` and its
listeners rely on. Instead, `LiftHeader` (package-private helper) replaces each trait as
one cascade of existing events:

1. build `new LiftTrait(newSpec)` with `parseValue(newSpec, old.getValue())`, detached;
2. `parent.addTrait(index(old), new)` → `ComponentAdded`;
3. move the annotations: `old.deleteAnnotation(a)`, `new.addAnnotation(a)` →
   `ComponentRemoved` / `ComponentAdded`;
4. `parent.deleteTrait(old)` → `ComponentRemoved`.

All of these already revert and reapply (`Slots`). The package rule "cascades are
reported in order" covers it: one `addFeatureSet` reports `FeatureSetAdded`, then the
`FeatureAdded`s and the trait replacements. A GUI that groups one action into one undo
step gets it whole. No new `ModelChange` type and no new `ModelProperty` for traits.

Cost: a rebound trait is a new object. A view holding the old one sees it removed; a
selection on it is lost. Rebinding is rare (declaring a name, adding or renaming a range).

**Fields: changed in place.** A field's storage (a MultiText, traits, annotations, dates)
does not depend on its definition, and replacing it would move a whole subtree. So
`LiftField` gets a tracked specification (`ModelProperty.FIELD_SPECIFICATION`) and the
header sets it.

**Header traits during a load.** Traits on ranges and range elements are read before
`<fields>`, and `option-range` is resolved only at `endHeader()`. Rebinding inside
`addFieldOrTraitDefinition` would come too early for an `option` definition (no range
yet: parse fails). So:
- `addFieldOrTraitDefinition` rebinds only when the dictionary is not loading. The
  decoder sets a flag, or calls an internal variant without rebinding.
- `LiftXmlDecoder.endHeader()` calls `header.rebindAll()` (internal) once the option
  ranges are set: every trait and field held by the header is checked against the
  resolution.
- No listener exists during a load, so nothing is reported.

The entries come after `</header>`: they resolve against the complete header and never
need rebinding.

## 6. Reader and writer

- **`LiftXmlDecoder`:**
  - `trait(...)` and `field(...)` keep going through the builders, which now resolve.
  - `fieldDefinition(...)`: the `containsFieldsAndTraitsDefinitions(name)` branch now
    meets only real double declarations (FieldWorks), never an invented definition. In
    the header, the only traits that can use it are header traits, and they are bound
    to it only after `endHeader()`. So `redeclared(...)`'s "in use" path becomes
    unreachable during a load. Keep it, and say so in a comment.
  - `endHeader()`: `rebindAll()` (§5).
- **`LiftWriterSession`:**
  - header: unchanged, it writes every definition, and they are all declared now;
  - traits and fields: `t.getName()`. A `RangeReference` writes the current range id.
- The **corpus round trip** must no longer add any `<field>` to the header. That is the
  test that shows the fix.

## 7. Impact on the code

**lift-api main**

| Where | Change |
|---|---|
| `LiftTrait`, `LiftField` | §2.2 |
| `LiftFieldAndTraitDefinition` | `implements`; `getDataModel()` without Optional (I1); `setKind` guard (§4) |
| `LiftHeader` | `resolve*`; `isUsed/countUses(FeatureSet)`; `uses(fd)` unchanged (`==`); rebinding in `addFieldOrTraitDefinition`, `addFeatureSet`, `renameFeatureSet`; `removeFeatureSet` check; `rebindAll()`; `holdsFeature` works through `getDataModel()` |
| `TraitBuilder`, `FieldBuilder` | resolve instead of get-or-create |
| `LiftXmlDecoder` | `endHeader()`; comment on `redeclared` |
| `LiftDictionaryCounterManager` | `TreeSet<LiftFieldAndTraitDefinition>` → set of `FieldSpecification` with a name comparator. **Existing bug**: the definition is not `Comparable`, so the second `add` throws `ClassCastException`; it never shows because `discover()` is never called. Fix or remove that dead code |
| `LiftDictionary.getValueCounterForTraitName` | `t.getName()` |
| `AbstractExtensibleWithField`, `AbstractLiftRoot.unlink`, `AbstractPropertyWriters` | `getName()` |
| `change/ModelProperty` | `FIELD_SPECIFICATION` |
| docs | `LiftTrait` class javadoc (the key is a specification, three cases), `LiftFieldAndTraitDefinition`, `model/package-info`, CLAUDE.md core design: one bullet on the invariant |

**DictionaryEditor** (updated with the API):

| Where | Change |
|---|---|
| `DictionaryValidator:49–56` | "undeclared field/trait" = `instanceof UndeclaredName` (was `getKind() == UNKNOWN`, which also flagged declared definitions with no type) |
| `TraitEditor:117–125` | switch on the specification; a `RangeReference` trait gets the range picker. **Behaviour change:** the picker is strict (`setValue`), so a new value can't be typed any more for 9 corpus trait names that are edited as free text today. I suggest the picker gets an explicit "new value…" action (`range.addFeature`), a GUI task outside this plan (I3) |
| `VariantEditor.variantTypeRange()` | can drop its guess-by-name: `getResolvedFeatureSet()` gives the range |
| `DictionaryQueries:127` | `TraitSpecification spec` |
| `ComponentColumns`, `CategoryViews`, `ObjectLabels`, `EntryColumns`, `EditorPresenters`, `FieldEditor` | `getName()`; `getTypeStr()` → `getDataModel()` |
| `FieldDefinitionsView` | lists declarations only; it no longer shows invented ones (that's what we want). It could show "undeclared names in use" with a "declare" button that calls `declareTraitDefinition` (rebinds them): a suggestion |
| `ComponentTablesTest:69` | `new LiftTrait(h.declareTraitDefinition("test-trait"), "a value")`: still fine (STRING) |

## 8. Tests

**Existing tests that change**
- `LiftHeaderTest:30`: `header_fieldDefinitionWithDiscoveredField.xml` expects 4
  definitions (3 declared plus the discovered `morph-type`). That becomes 3, and the
  `morph-type` trait is `UndeclaredName` (the file has no range).
- Tests that call `getOrCreateTraitsDefinitions` before `new LiftTrait(def, "x")`
  (`AttachmentTest`, `DeletionTest`, `ChangeEventsTest`, `RegressionTest`,
  `BuilderTest`, `ChangeRoundTripTest`) keep working: the definition is declared,
  STRING. Only the rename (I2) touches them.

**New**
- *Corpus*:
  - after load, the 9 range-named traits are `RangeReference` with feature values;
  - `countUses(morph-type's "stem")` > 0;
  - load→save adds no `<field>` (compare the header's definition names with the
    file's).
- *Resolution*: definition wins over range; a FIELD definition is skipped for a trait;
  fields never resolve to a range.
- *Header operations* (§4), each with the invariant checked:
  - declaring a name used by range or undeclared traits;
  - `addFeatureSet` over undeclared traits;
  - `renameFeatureSet`: written under the new id and read back to the same set; refused
    over a definition name;
  - `removeFeatureSet` refused while a range-keyed trait (even an empty one) points at
    it;
  - an unparsable value makes a declaration throw and change nothing.
- *Reader*: a header trait followed by its `<field-definition type="option"
  option-range=…>` ends bound to the definition, with a feature value.
- *Undo*: `ChangeRoundTripTest` gets operations for range-keyed and undeclared traits
  and for a declaration that rebinds. `ModelSnapshot` records each trait's and field's
  specification kind and name (`def:x`, `range:x`, `name:x`), so that a revert that
  leaves the wrong kind fails.
- *Round trip*: `DictionarySnapshot` compares the header definitions (name, kind, data
  model, range, targets), and the specification kind of traits and fields.
- `RegressionTest`: "range-named traits hold features and are counted".

## 9. Questions (recommended answer first; the plan assumes it)

- **Q1. Range case:** a `RangeReference(FeatureSet)` record (recommended), or `FeatureSet`
  implements `TraitSpecification` directly?
- **Q2. Rebinding a trait:** replace the trait object, built from existing events
  (recommended), or change its specification in place? In place keeps the object, but
  needs a new event that carries the specification and the value together, and swaps
  the value properties under the GUI's bindings.
- **Q3. A definition of the other kind** (a `multitext` definition named like a trait):
  skip it in the resolution with a warning (recommended), or keep failing as today?
- **Q4. Rebind automatically** in `addFieldOrTraitDefinition` / `addFeatureSet` /
  `renameFeatureSet` (recommended: otherwise the model differs from its own reload), or
  leave the traits as they are and document it?
- **Q5. An `option` definition with no `option-range`.** Today its traits fail to parse,
  and the load fails. Should it take the range of its own name when there is one (LIFT:
  "resolved by its use in a field-definition")? The writer would then add the
  `option-range` attribute. Suggested as a separate small step, not required by this
  plan.

## 10. Steps

| Step | Content | Notes |
|---|---|---|
| S1 | Types of §2.1; `getDataModel()` without Optional (I1); definition `implements` | compiles with the old callers adapted |
| S2 | `LiftTrait` / `LiftField` on the new types, `getName()`; `FIELD_SPECIFICATION` | |
| S3 | `resolve*`; builders use them; `declare*` rename (I2) | corpus now loads range-named traits as features |
| S4 | §4 guards and rebinding, §5 cascade; `isUsed/countUses(FeatureSet)` | |
| S5 | Reader `endHeader()` / `rebindAll()`; `redeclared` comment | |
| S6 | Call sites lift-api; `LiftDictionaryCounterManager` fix or removal | |
| S7 | Tests §8; docs (javadoc, `package-info`, CLAUDE.md) | `mvn clean test` |
| S8 | DictionaryEditor call sites, `DictionaryValidator`, `TraitEditor`, `VariantEditor` | GUI "new value…" (I3) separately |

## 11. Options that were weighed

- **First plan: implicit definitions** (`trait-type-resolution-2026-10.md`). There is
  one definition per name with an `implicitness` flag, and the writer skips the
  implicit ones. The three cases share the definitions' namespace, which causes:
  - duplicates on a later declaration;
  - edits lost on save;
  - the range pinned by a hidden definition;
  - the rename link broken on reload.
- **B: one definition per name, with "declared" derived from what declared it.** No API
  change, but it treats symptoms (the range is still pinned, and the data model can't
  change while used).
- **C: name plus string value, range looked up when needed.** Rejected: values would no
  longer be `Feature` objects (against "whatever is keyed by a type holds the object").

## 12. Found on the way (not in this plan)

- `LiftDictionaryCounterManager.discoverFields` / `attachTargetOnField`: a `TreeSet` of
  non-`Comparable` definitions (dead code today, §7).
- **Done (2026-10-21):** `setResolvedRange` is refused while traits use an option
  definition, and `resolvedRangeProperty()` / `kindProperty()` are read-only. Before
  that, `setResolvedRange` had no guard. Changing the range of a
  definition in use leaves its traits holding features of the old set. Refuse it while
  used, as `setDataModel` does. A `TODO.md` entry.
- Built-in ranges declared with an `href` lose it on save and are written inline (first
  plan). A `TODO.md` entry.
