# Resolving the name of a trait: range, definition, or neither — plan (2026-10-20)

## Why

`TraitBuilder` gives a trait its definition with
`LiftHeader.getOrCreateTraitsDefinitions(name)`: the definition of that name, or a new
trait definition with the `STRING` data model. That ignores what the LIFT specification
says a trait is (p. 13): "a reference to a single range-element in a range [...] Where no
range is linked the name is informal or resolved by its use in a field-definition". A
trait named after a range is a reference to that range, even when no
`<field-definition>` says so.

Consequences today, on the reference corpus (`20240828Lift.lift`, measured on HEAD
`cd3998a`):

- **Values are strings, not features.** 9 trait names are range ids (`morph-type` with
  3,301 traits, `semantic-domain-ddp4` 854, `Noun-infl-class` 121, `inflection-feature` 117,
  `Verb-slot` 107, `Noun-slot` 28, `from-part-of-speech` 10, `status` 2). Their definitions
  are created on the fly as `TRAIT`/`STRING`, with no range.
- **`LiftHeader.isUsed(Feature)` / `countUses(Feature)` miss them** (`holdsFeature` only
  looks at `FEATURE*` data models), so `FeatureSet.removeFeature` and `removeFeatureSet`
  let a user delete a feature that thousands of traits use. `changeFeatureId` does not
  reach them either; DictionaryEditor works around it by rewriting the strings
  (`DictionaryQueries.renameFeature`).
- **A save declares what the file never declared.** Every definition created on the fly
  is written: a load-save of the corpus adds 14 `<field tag="...">` to the header (the
  12 trait names, plus `note` and `summary`, fields used without a definition, created
  by `FieldBuilder` through `getOrCreateFieldDefinitions`).
- DictionaryEditor already guesses the range from the name in one place
  (`VariantEditor.variantTypeRange()`), a sign the model should do it.

---

## 1. The change requested

Resolve the name (`type`) of a trait in three cases, and record which one applied in a
new property of `LiftFieldAndTraitDefinition`, `implicitness`, of a new enum
`DefinitionImplicitness`:

| Case | The name is... | Definition | `implicitness` |
|---|---|---|---|
| 1 | the id of a feature set (range) | created: kind `TRAIT`, data model `FEATURE`, range = that set | `IMPLICIT_DEFINITION_WITH_DIRECT_RANGE_REFERENCE` |
| 2 | the name of a definition | that definition | unchanged (`EXPLICIT_DEFINITION` for a declared one) |
| 3 | neither | created: kind `TRAIT`, data model `STRING` | `IMPLICIT_DEFINITION_WITH_STRING` |

The writer skips every definition whose `implicitness` is not `EXPLICIT_DEFINITION`.

---

## 2. Findings: what the change, as written, would break

**F1. The order of the cases must start with the existing definitions.** As written,
case 1 comes first: the second `morph-type` trait would find the range again and create
a second definition named `morph-type`, which `addFieldOrTraitDefinition` refuses
(`Duplicate field or trait definition`), so the corpus would not load. Definitions are
keyed by name, so whatever case 1 creates is found by case 2 next time. The working
order is:
1. a definition of that name exists (declared, or created by an earlier trait) → use it;
2. otherwise a feature set of that id exists → create the case-1 definition;
3. otherwise → create the case-3 definition.

This only changes the outcome when a file declares both a range `X` and a
`<field-definition name="X">`. The declared definition then wins, which is right: it can
say `option-collection`, or point at another range with `option-range`. See D1.

**F2. Editing an implicit definition would be lost on save.** The header views
(DictionaryEditor `FieldDefinitionsView`) let a user give a definition a label, a
description, targets, a writing system. On an implicit one, the writer would skip it and
drop all of that without warning. See D2.

**F3. Renaming a range breaks its case-1 definitions on reload.** Since `cd3998a`,
`renameFeatureSet` renames a range and the definitions follow the object. A case-1
definition keeps its name (`morph-type`, final) while its range becomes, say,
`morph-types`. The traits are still written `name="morph-type"`, the definition is
skipped, and on reload `morph-type` matches no range: the traits fall to case 3 and lose
their range. See D4.

**F4. A range can no longer be removed once a trait has named it.**
`removeFeatureSet` refuses a set that a definition takes as its range. A case-1
definition stays after its last trait is deleted, so the range stays blocked by a
definition that appears nowhere in the file. See D5.

**F5. Loading fails when a trait in the header precedes the definition of its name.**
The reader builds the traits of range elements while it reads `<ranges>`, before
`<fields>`. Such a trait creates a case-1 or case-3 definition, then the
`<field-definition>` of that name arrives. `LiftXmlDecoder.fieldDefinition` treats it as
a duplicate and removes the first definition, which `removeFieldOrTraitDefinition`
refuses because a trait uses it: the load fails. **This already happens today** with
`getOrCreate`; the corpus has no trait in its header, which is why nothing shows it. The
change keeps the problem; the fix is part of this plan (P3.4).

**F6. The resolution depends on what exists when the trait is built.** In a file the
header comes first, so the result is stable. Through the API it is not:
- a trait built before the range of its name gets a case-3 definition, and keeps it
  when the range is added. A `STRING` definition cannot become `FEATURE` while traits
  use it (`setDataModel` refuses: their values are held in a string property). After a
  save and a reload, the same traits resolve to case 1.
- adding a declared definition (`FieldDefinitionBuilder`, `createTraitDefinition`) whose
  name an implicit one already has fails with "Duplicate".

The second point needs a fix (P3.3); the first can be documented. See D6.

**F7. Inline ranges gain range elements on save.** With `FEATURE`, a value that the
range does not contain is added to it (`LiftTrait.parseValue`, lenient, as for declared
option definitions). For a range declared in the header without `href`, those features
are written back as `<range-element>`s the file did not have. On the corpus every case-1
range has an `href` to a `.lift-ranges` file that the reader does not load, so they are
only written as references and nothing changes. It is still a visible behaviour change
for other files. It is consistent with "the file is the authority on its values", which
the reader already applies to declared option definitions.

**F8. The external ranges are not loaded, so a case-1 range only holds the values seen
in the data.** The DictionaryEditor `TraitEditor` will show its range picker (data model
`FEATURE` + resolved range) instead of the editable combo. The values offered are the
same (those already used), but a new value can no longer be typed: `setValue` is strict
and refuses an unknown feature. This is a GUI regression to accept or work around, not
a library bug.

**F9. `getOrCreateTraitsDefinitions` must not change.** It is public and called directly,
followed by `new LiftTrait(def, "a value")` (lift-api tests: `DeletionTest`,
`AttachmentTest`, `RegressionTest`, `ChangeEventsTest`, `ChangeRoundTripTest`;
DictionaryEditor's `ComponentTablesTest`). That constructor fills the string property.
If the definition came back `FEATURE`, the property would be `null` and the constructor
would throw a `NullPointerException`. The three-case resolution must be a new method,
used by `TraitBuilder`.

**F10. `isDataModelDeclared()` would be true for a case-1 definition.**
`setDataModel(Optional.of("option"))` marks the data model as declared, which is what
the writer will need if the definition is ever made explicit (D2, D4): it must then
write `type="option" option-range="..."`. It means "declared" now reads "known", not
"written in the file": the javadoc must say so, and views should read `implicitness`.
The `setDefinitionType` named in the request is deprecated; use `setDataModel`.

**F11. Fields have the same problem.** `note` and `summary` (corpus) are fields without
a definition; `FieldBuilder` creates one, and the writer adds it to the header. The
request is about traits, but the same `implicitness` would stop the writer from
inventing those (case 3 for fields; a field cannot reference a range, so there is no
case 1). See D3.

**F12. `implicitness` must be reported if it can change.** If editing an implicit
definition makes it explicit (D2, D4), that is a value change: it needs a `ModelProperty`
and a tracked property, or undo leaves the definition explicit (checklist step 10;
`ChangeRoundTripTest`). If it can never change, a final field is enough.

**F13. Nothing in the round-trip tests looks at the header definitions.**
`DictionarySnapshot` compares entries only, so neither today's 14 extra definitions nor
their removal is visible to `RoundTripTest`. A header check is needed (P4).

What the change fixes, as the request expects: case-1 traits hold `Feature` objects, so
`isUsed(Feature)`, `countUses`, `removeFeature`, `removeFeatureSet` and
`changeFeatureId` all take them into account. The corpus header comes back as it was
read: no invented definitions.

Unrelated, found on the way: the built-in feature sets that the corpus declares with an
`href` (`etymology`, `grammatical-info`, `lexical-relation`, `note-type`,
`reversal-type`, `translation-type`) lose that `href` on save and are written inline
with the values seen in the data. `LiftXmlDecoder.range` ignores the `href` of a range
the header already has. This deserves its own entry in `TODO.md`.

---

## 3. Decisions to take

**D1. When both a definition and a range have the name.** (a) The definition wins
(order of F1). (b) The range wins, as the request orders it; then case 1 must not create
a definition (one already has the name), and the trait would have to point at the range
in some other way. **Recommended: (a).**

**D2. Editing an implicit definition.** (a) Any change made through its public setters
(label, description, targets, writing system, data model, range) makes it
`EXPLICIT_DEFINITION`: the user has declared it. (b) A public `setImplicitness`, and
the views decide. (c) The writer writes an implicit definition that has content.
**Recommended: (a)**, with the change reported (F12).

**D3. Fields.** Mark the definitions `FieldBuilder` creates on the fly as
`IMPLICIT_DEFINITION_WITH_STRING` too. **Recommended: yes.** Otherwise the header keeps
gaining `note`, `summary`.

**D4. Renaming a range that case-1 definitions use.** (a) Those definitions become
explicit: the writer then writes
`<field-definition name="morph-type" type="option" option-range="morph-types">`, and the
link survives. (b) Refuse the rename. **Recommended: (a).**

**D5. Unused implicit definitions.** (a) `removeFeatureSet` removes the unused implicit
definitions that point at the set, and ignores them in its refusal. (b) Remove an
implicit definition when its last trait goes. That means listening to deletions, and
undo becomes harder. **Recommended: (a).**

**D6. A range added after traits named after it (F6, first point).** (a) Document it;
the traits keep their string values until a reload. (b) Convert them: re-parse each
value into a feature of the new range, and swap the definition. **Recommended: (a)**
for this plan, (b) as a `TODO.md` entry.

**D7. Names.** `implicitness` and the three values as requested, or shorter ones
(`DECLARED`, `RANGE_NAME`, `UNDECLARED`). Your call; the plan uses the requested names.

---

## 4. Work plan

**P1. Model**
1. New enum `model/DefinitionImplicitness`: `EXPLICIT_DEFINITION`,
   `IMPLICIT_DEFINITION_WITH_DIRECT_RANGE_REFERENCE`, `IMPLICIT_DEFINITION_WITH_STRING`.
   Its javadoc cites the spec (p. 13) and says what the writer does with each value.
2. `LiftFieldAndTraitDefinition`:
   - `implicitness`, default `EXPLICIT_DEFINITION`, so every existing creator (reader,
     `FieldDefinitionBuilder`, `createTraitDefinition`...) is unchanged;
   - `getImplicitness()`, a read-only `implicitnessProperty()`, a package-private setter;
   - if D2(a): the public setters make it explicit. One `TrackedProperty` and a
     `ModelProperty.DEFINITION_IMPLICITNESS`, so undo restores it.
   - Javadoc of `isDataModelDeclared` (F10).
3. `LiftHeader`:
   - new `resolveTraitDefinition(String name)`, the three cases in the order of F1;
     javadoc with the table of §1;
   - new `resolveFieldDefinition(String name)` (if D3): existing, or case 3;
   - `getOrCreateTraitsDefinitions` / `getOrCreateFieldDefinitions` unchanged (F9); their
     javadoc points at the new methods;
   - `removeFeatureSet` per D5;
   - `renameFeatureSet` per D4.

**P2. Builders**
1. `TraitBuilder.definition` → `resolveTraitDefinition`. Javadoc of the class: how
   the name is resolved.
2. `FieldBuilder` → `resolveFieldDefinition` (if D3).

**P3. Reader and writer**
1. `LiftWriterSession.writeHeader`: skip non-explicit definitions. If none is left, do
   not write an empty `<fields>`.
2. Nothing to change for traits in entries: the header is read first.
3. Declaring a definition whose name an implicit one has (F6, second point), in
   `LiftHeader.addFieldOrTraitDefinition`:
   - the implicit definition is *promoted* in place: it takes the declared attributes and
     becomes explicit;
   - this is refused only if the declared data model differs while traits use it.
   - `FieldDefinitionBuilder.build` and `createTraitDefinition` go through it.
4. `LiftXmlDecoder.fieldDefinition` (F5): when the existing definition is implicit,
   promote it through P3.3 instead of removing it. Keep the current "declared twice, the
   last one wins" only for two explicit declarations.

**P4. Tests**
- `RegressionTest`:
  - a trait named after a range holds a `Feature` and is counted by
    `countUses(Feature)`;
  - `removeFeature` refuses a feature that such a trait uses;
  - `changeFeatureId` changes the written value;
  - a trait named after a declared definition uses it (case 2), including when a range
    has the same name (D1);
  - a trait with an unknown name gets a `STRING` definition (case 3), and repeated
    traits share one definition (F1);
  - a load-save of a file with case-1 and case-3 traits writes no definition for them;
  - a header trait followed by the `<field-definition>` of its name loads (F5).
- `RoundTripTest`: compare the header definitions too (name, implicitness, data model,
  range) (F13). The corpus must come back with its 7 declared definitions and no other.
- `AttachmentTest`-like check: building a declared definition over an implicit one
  promotes it (P3.3).
- If D2/D4: `ChangeRoundTripTest` operations "edit an implicit definition" and "rename
  a range used by a case-1 definition", reverted.
- If D5: removing a range whose only users are unused implicit definitions succeeds.
- `LoadingCostTest`: one extra map lookup per trait; check that the load time stays
  within noise.

**P5. Docs**
- CLAUDE.md, "Core design": one bullet on how a trait name is resolved, and on the
  definitions the writer skips.
- `LiftTrait` class javadoc: the spec quote is already there; add the resolution.
- `TODO.md`: D6(b), and the built-in ranges that lose their `href` (§2, last paragraph).

**P6. DictionaryEditor (separate repository, after this lands)**
- `VariantEditor.variantTypeRange()` can rely on the definition's range.
- `DictionaryQueries.renameFeature` no longer needs to rewrite case-1 values, which follow
  the feature. Its string rewrite still splits on spaces, which predates the comma format
  (D2 of `builder-only-construction-2026-10.md`), and should split on commas.
- `TraitEditor`: decide about F8 (allow a new value in the range picker, through
  `parseValue` or by adding the feature first).
- `FieldDefinitionsView`: show `implicitness`, or hide implicit definitions.

---

## 5. Risks

- **Files:** the header no longer gains invented definitions (wanted). Inline ranges may
  gain range elements (F7). Trait values are written the same: a `FEATURE` value is
  written as its id.
- **Applications:** a trait named after a range now has a `FEATURE` value.
  `stringValueProperty()` returns `null` for it; `getValue()` / `setValue()` keep
  working, but `setValue` becomes strict (F8). Code that reads `stringValueProperty()`
  for any trait breaks for those names.
- **Load time:** negligible (one `HashMap` lookup per trait, and fewer strings kept).
