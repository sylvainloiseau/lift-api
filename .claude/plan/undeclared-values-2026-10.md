# Undeclared values and undeclared ranges — plan

Status: **implemented** (2026-10-22) in lift-api and DictionaryEditor — Q1–Q4 and Q6 as
suggested; Q5: an `option-range` naming a range the file does not declare keeps failing the
load (with a clearer message). See §8 for what differs from the plan.

## 1. The problem

The contract is that loading a file and saving it gives back the same document. Today it
is broken in three ways (checked by loading small files and saving them unchanged):

1. **A value a range defined in the header does not declare.** For example
   `<trait name="colours" value="blue"/>` where `colours` declares only `red`, or a note
   type `reference` that `note-type` does not declare. The reader and the builders create
   the feature silently, and the writer writes it:
   `<range id="colours"><range-element id="blue"/><range-element id="red"/></range>`.
   For a range defined in an external file, the same value is flagged
   (`Feature.isUndeclaredInTheExternalFile()`) and not written: that case is already right.
2. **A built-in range the file does not declare** (`grammatical-info`, `note-type`, …),
   when entries use values of it. The built-in set gets the values, and is written with
   them: `<range id="grammatical-info"><range-element id="Noun"/></range>`.
3. **The built-in ranges are always written**, even empty and unused
   (`<range id="annotation-type"></range>`, `<range id="etymology"></range>`…). The header
   creates them by default (they are hard-coded) and the writer writes every set.

A fourth, related case fails differently: a definition whose `option-range` names a range
the file does not declare cannot be loaded at all (`Range not found` at the end of the
header). That stays an error (Q5); only the message improves (§4.6).

## 2. Goal (from the user's answer)

- **Undeclared values.** A feature that its range does not declare is flagged as
  *undeclared in its range*, whatever the range: internal, external, or undeclared.
  - It cannot be modified and is not written.
  - The reader warns about it, once per range and value.
  - The validator reports it.
  - An operation **declares** it: it can then be modified, and is saved.
- **Undeclared ranges.** A built-in range the file does not declare is flagged as
  undeclared.
  - It cannot be modified and is not written, nor are its values.
  - The reader warns about it once, when it is first needed, not at every occurrence.
  - An operation declares it: it can then be edited, and is saved.
- **DictionaryEditor.**
  - Undeclared values are shown in grey in the tables and trees of ranges and range
    values; undeclared ranges are shown in grey too.
  - A button declares a value, another declares a range.

## 3. Today

- **Undeclared values are created by** `FeatureSet.getOrCreateUndeclaredFeature`, reached
  through `ModelInternals`:
  - by the builders, through `AbstractLiftElementBuilder.typeFeature`: types of notes,
    relations, etymologies, reversals, translations and annotations, and parts of speech;
  - by the lenient parse of `LiftTrait`;
  - by the decoder's `parent` resolution at `endHeader`.
- **The flag.** `Feature.isUndeclaredInTheExternalFile()` is set only when the set is
  external.
- **The guard.** `ExternalDefinitionGuard.requireWritable` refuses changes of external sets
  only.
- **The writer.** For an external set, `LiftWriterSession.writeHeaderRange` writes the
  reference and the inline content. For any other set it writes everything: all the sets,
  and all their features.
- **The built-in sets** are created by the `LiftHeader` constructor, with no notion of
  "declared".

## 4. Design

### 4.1 Declared or not

| | Declared when… | Undeclared when… |
|---|---|---|
| **Feature** | a `<range-element>` declares it (header, external file, or inline in a `<range href>`), or it is created explicitly: `FeatureSet.addFeature`, `FeatureBuilder`, `LiftDictionaryBuilder` | the reader or a builder creates it for a value its range does not declare |
| **Feature set** | the file has its `<range>` (inline or `href`), or it is created explicitly (`LiftHeader.addFeatureSet`, `FeatureSetBuilder`), or it is declared (§4.4) | it is a built-in set the file does not declare. Only built-in sets can be undeclared: an `option-range` naming a range the file does not declare is an error (Q5) |

**Model:**
- **Values.** `Feature.isDeclared()` replaces `isUndeclaredInTheExternalFile()`; its value
  is the opposite, and it is renamed because it now holds for any range.
- **Ranges.** `FeatureSet.isDeclared()` is new. Each built-in set starts undeclared; the
  reader declares it when it reads its `<range>`.
- **Both flags are real model state, not derived.** They are reported (§4.5), because
  declaring is an undoable change.

**Q1** decides what "declared" means for a dictionary created from scratch rather than read.

### 4.2 Read-only

`ExternalDefinitionGuard` becomes `DefinitionGuard` (or keeps its name; Q6). It refuses a
change when:
- the owning set is external (as today);
- or the owning set is undeclared;
- or the owning **feature** is undeclared. The walk up from the changed object stops at the
  first `Feature` and checks it, then goes on to its set.

What this means:
- **An undeclared feature** cannot change: its id (`changeFeatureId`), guid, superordinate
  feature, label, abbreviation, description, traits, fields and annotations.
- **The other features of a declared internal set stay editable.** So does the set itself:
  adding a feature, editing its texts.
- **Removing an undeclared feature** is allowed when nothing uses it: that is not a change
  of the feature, and the reload would not create it anyway. It is refused while it is used,
  as for any feature.
- **Naming an undeclared feature as the superordinate of a declared one is allowed.** Only
  the declared feature changes. It is written as `parent="x"`, and the reload creates `x`
  undeclared the same way.
- **An undeclared set** refuses everything, like an external one. The exception is the bare
  features the reader and builders create, through the existing suspension of the guard.
- **The existing suspensions are unchanged**: the reader, rebinding, change revert/reapply,
  and the bare features.

Error message: "The value x of the range y is not declared; declare it to change it", and
"The range y is not declared in the file; declare it to change it".

### 4.3 Writing

- **An undeclared set is not written at all**, neither its content nor a reference.
  - Its values come back on reload, from the components that use them.
  - A definition's `option-range` naming it (only a built-in set can be undeclared) is
    still written. On reload the name resolves to the built-in set again, undeclared again.
- **A declared internal set** is written with its declared features only.
- **A declared external set** is written as today: the reference and the inline content.
  Inline content is always declared.
- **A declared feature whose `parent` is an undeclared feature** is written with
  `parent="x"`. The reload creates `x` undeclared again, so the result is the same.

### 4.4 Declaring

- **`LiftHeader.declareFeatureSet(FeatureSet)`**
  - Declares an undeclared set: it becomes editable, and is written (empty, with only the
    values declared later).
  - Its undeclared values stay undeclared.
  - Refused for a set that is declared, or not in the header.
- **`LiftHeader.declareFeature(Feature)`**
  - Declares an undeclared value: it becomes editable and is written.
  - In an undeclared set, it declares the set too, in the same operation (Q3).
  - In an external set: see Q2.
- **Names.** These match the existing `declareTraitDefinition` / `declareFieldDefinition`
  of the header.
- **No automatic declaration.** An undeclared feature is not declared because someone uses
  it, nor because a label is given to it: that is refused (read-only). It is declared only
  explicitly. This replaces the earlier idea "declared when it is given a label".

### 4.5 Reporting and undo

- **Two new `ModelProperty<Boolean>` constants**, `FEATURE_DECLARED` and
  `FEATURE_SET_DECLARED`. They are held in `TrackedProperty` fields of `Feature` and
  `FeatureSet`, which are not guarded, like `HREF`.
- **Undo.** Declaring is reported as a change from `false` to `true`. Reverting it makes the
  feature or set undeclared, and read-only, again.
- **Their appliers** go through `ModelInternals` (as for `HREF`). There is no public setter
  to make something undeclared: it would hide declared content from the writer.

### 4.6 Reading and warnings

- **When a built-in set is declared.** The decoder declares a built-in set when it reads its
  `<range>`, inline or with an `href`. New sets created by the decoder are declared, through
  the builder.
- **An undeclared value warns once per (range, value).** The warning names the range, the
  value and the line of the first use, e.g. `The value "blue" is not declared in the range
  colours (line 12); it is kept, but not saved in the header.` The set of values warned
  about lives in the decoder and is cleared at the end of the document.
  - This covers values seen while reading entries, header traits, and the `parent` of range
    elements.
  - Values created later, by the builders while editing, do not warn: they are not reading.
- **An undeclared range warns once, when it is first needed.** That is the first bare value
  created in it, or the first `option-range` that names it. For example: `The range
  grammatical-info is not declared in the file (line 40); its values are kept, but not
  saved.`
- **An `option-range` naming a range that is neither declared nor built in is an error**
  (Q5), as today. The message improves: today it is `Range not found: x`, thrown at the end
  of the header with no position. It becomes `The definition y takes its values from the
  range x, which the file does not declare`, with the line of the definition (recorded when
  the definition is read, since the check happens at `</header>`).
- **The warnings are collected** (`LiftDictionary.getLoadWarnings()`?) as well as logged, so
  that DictionaryEditor can show them after opening a file (Q4).

### 4.7 Validator (DictionaryEditor)

- `validate.valueNotInExternalRange` becomes **`validate.undeclaredValue`**, for any range:
  "Values not declared in their range". The entry gives `range = value`.
- **`validate.undeclaredRange`** is new: "Ranges used but not declared". It lists the
  undeclared sets that hold values.
- **`validate.externalRangeMissing`** stays.

### 4.8 DictionaryEditor

- **`FeatureSetView` (the type tables) and `RangeView` (the range tree):**
  - undeclared values in grey: a CSS class `undeclared` on the row or cell, defined in
    `app.css` with a grey text colour; the tooltip says "not declared in the range";
  - a button **"Declare this value"**, enabled when the selected value is undeclared. It
    calls `declareFeature`, is undoable, and refreshes the view;
  - the editors of an undeclared value (label, abbreviation, description) are read-only, as
    for an external range.
- **An undeclared range** (in its view, and in the navigation tree):
  - the navigation item of the range is in grey, and so is its type page;
  - a banner says "Not declared in the file: read-only, not saved", with a button
    **"Declare this range"** (`declareFeatureSet`, undoable);
  - the controls are disabled, as for an external range (shared code with the external
    banner in `HeaderViews`).
- **`ChangeLabels`**: "declaring the value x", "declaring the range y".
- **Load warnings** (Q4): after opening a file, a dialog or the status bar lists the
  undeclared ranges and values found (one line per range, with a count of values), with a
  link to the validator report.
- **Messages** in English and French.

### 4.9 The corpus and the demo dictionary

- **Corpus.** All the corpus' ranges are external, and its undeclared values (part of
  speech, note type `reference`, relation `_component-lexeme`, inflection features) are
  already undeclared and not written. What changes is the built-in sets the corpus does not
  declare (`annotation-type`): they are no longer written. The load → save → reload snapshot
  is unchanged, since the set is undeclared in both.
- **Demo dictionary** (DictionaryEditor): its type pages may show undeclared ranges. The UI
  tests that edit them declare them first, as they make external ranges internal.

## 5. Steps

1. **lift-api model:**
   - `Feature.declared`, `FeatureSet.declared` (`TrackedProperty<Boolean>`),
     `FEATURE_DECLARED` and `FEATURE_SET_DECLARED`;
   - `isDeclared()` replaces `isUndeclaredInTheExternalFile()`;
   - the guard extended (§4.2);
   - `declareFeature` / `declareFeatureSet`;
   - explicit creation declares; the bare-feature path does not.
2. **lift-api reader:** built-in sets declared on their `<range>`; the warnings, once each,
   and collected (Q4); the clearer error for an `option-range` naming a range the file does
   not declare (Q5).
3. **lift-api writer:** skip undeclared sets and undeclared features.
4. **lift-api tests** (§6), docs (`FeatureSet`, `Feature`, `LiftHeader`, `package-info`,
   CLAUDE.md), `mvn clean test`, `mvn install`.
5. **DictionaryEditor:** grey style, the two buttons, banner, read-only editors, validator,
   labels, messages, load warnings; tests.

## 6. Tests

**lift-api** (`UndeclaredValuesTest`, new, plus `RegressionTest`):
- **Load → save keeps the header** for the three cases of §1: an inline range with an
  undeclared value, an undeclared built-in range with values, and the built-in ranges never
  declared and unused (none written). Pinned in `RegressionTest`, as the bug fix.
- **The flags:** `isDeclared()` for features of internal, external and undeclared sets; for
  explicitly added features; for `LiftDictionaryBuilder` / `FeatureSetBuilder` /
  `FeatureBuilder`.
- **Read-only:** every guarded change of an undeclared feature and of an undeclared set
  throws. The other features of the set stay editable. An unused undeclared feature can be
  removed.
- **Declaring:** after `declareFeature` / `declareFeatureSet`, edits work and the save
  writes the value or range. Declaring a value of an undeclared set declares the set. Both
  are refused when already declared. Undo and redo.
- **Warnings:** once per range and value, once per range, with the line.
- **Q5:** an `option-range` naming a range the file does not declare fails the load; the
  message names the definition, the range and the line. An `option-range` naming an
  undeclared built-in range loads, and the range stays undeclared.
- **`ChangeRoundTripTest`:** an operation "declare a value / declare a range". `ModelSnapshot`
  and `DictionarySnapshot` include the flags.
- **`ExternalRangesTest`:** follows the renaming of `isUndeclaredInTheExternalFile`.

**DictionaryEditor:** the validator keys; a UI test that a value is shown undeclared (style
class) and declared by the button; a UI test that an undeclared range is declared by its
button.

## 7. Questions (answered: the suggested answer (a), except Q5)

- **Q1 — A dictionary created from scratch** (`LiftDictionary.makeBuilder()`, "New
  dictionary" in DictionaryEditor). Its built-in sets exist but no file declares them.
  - **(a) suggested:** they start undeclared, as for a file. `LiftDictionaryBuilder` declares
    the sets it fills (`addTypes`, the parts of speech), since that is explicit. In the
    editor, the type pages of a new dictionary show the "Declare this range" banner. One rule
    for every dictionary, and an empty new dictionary is saved without empty ranges.
  - (b) they start declared: they are written empty, as today.
  - (c) an undeclared set becomes declared by the first explicit change instead of refusing
    it. Simpler for the user, but it contradicts "not modifiable" for read files, so the rule
    would differ between read and new dictionaries.
- **Q2 — Declaring a value of an external range** (one the shared file does not declare).
  - **(a) suggested:** refused. The range must be made internal first, then the value
    declared. In the GUI, the "Declare this value" button of an external range does both
    after one confirmation. This keeps the rule that what the `.lift` file adds to an
    external range is only what it read inline.
  - (b) declared as an inline addition to the external range, written after the reference
    and read-only like the other inline elements. It keeps the link to the shared file, but
    the value can never be given a label without making the range internal.
- **Q3 — Declaring a value of an undeclared range.**
  - **(a) suggested:** it declares the range too, in one undoable operation.
  - (b) refused until the range is declared.
- **Q4 — Where the warnings go.**
  - **(a) suggested:** logged (as all reader warnings), and also collected on the dictionary
    (`LiftDictionary.getLoadWarnings()`, a read-only list of messages), which DictionaryEditor
    shows after opening a file.
  - (b) logged only. DictionaryEditor relies on the validator report.
- **Q5 — An `option-range` naming a range the file does not declare** (today: the load
  fails). **Answered by the user: (b), keep failing**, with the clearer message of §4.6.
  (The suggestion had been (a), an undeclared range created with a warning.) A built-in
  range is not concerned: it always exists, and is only undeclared.
- **Q6 — Naming.**
  - **(a) suggested:** `isDeclared()` on `Feature` and `FeatureSet`, and
    `LiftHeader.declareFeature` / `declareFeatureSet`. `ExternalDefinitionGuard` renamed
    `DefinitionGuard`, since it now also guards undeclared definitions.
  - (b) other names you prefer.

## 8. Implementation notes (differences from the plan)

- **Warnings in an undeclared range**: one warning for the range, not one per value (a
  file that does not declare `grammatical-info` would otherwise warn once per part of
  speech). Values of declared ranges warn once each. The reader learns of each bare feature
  through a listener the decoder sets on the header (`ModelInternals.setUndeclaredFeatureListener`),
  so the warning has the line of the first use.
- **Load warnings** (`LiftDictionary.getLoadWarnings()`) also hold the warnings about
  external ranges that cannot be found and range elements declared twice. They are kept by
  `DictionaryMutator.recordLoadWarning`.
- **A bare feature declared later in the file** (a value used before its
  `<range-element>`, e.g. a trait of a range element naming a feature of a range further
  down) is declared when its `<range-element>` comes, and filled in place.
- **`changeFeatureId`** checks the feature itself (an undeclared one cannot be renamed),
  not only its set.
- **The writer** writes no `<ranges>` at all when no range is declared.
- **DictionaryEditor**: the declare actions go through `EditorContext.perform` (one undo
  step each, with their own label). The edits of the type tables go through the model
  rather than `formTextProperty` (whose listener would swallow a refusal), and a refusal
  says to declare the value first. The load warnings are shown after "Open" (not for the
  demo dictionary). The UI tests that edit the demo dictionary's built-in ranges declare
  them first.
