# External ranges files (`<range id="…" href="…"/>`) — plan

Status: **implemented** (2026-10-22) in lift-api and DictionaryEditor — Q1–Q4 and Q6 as
suggested; Q5 answered by the user: inline children of a `<range href>` override the file
(§4.8). See §10 for what differs from the plan.

## 1. Goal

- **Reading.** When the reader meets `<range id="X" href="F"/>` in the header, it reads the
  ranges file `F`. Each file is parsed once per load and kept until the end of the header.
  The content of range `X` is what the `FeatureSet` `X` holds.
- **Read-only.** A feature set defined in an external file cannot be modified in memory: not
  its id, texts, features, hierarchy, or the traits and fields it holds.
- **Making it internal.** One operation makes the set internal: it removes the `href`, and
  the set becomes writable and is written in full in the header.
- **Writing.** The writer writes an external set as `<range id="X" href="F"/>` only, and never
  writes the ranges file.
- **DictionaryEditor** shows external sets as read-only, with a button that makes them
  internal.

## 2. Today

- **Reader.** `LiftXmlDecoder.range` keeps the `href` of a new set and reads nothing else from
  the file. For a built-in set (`grammatical-info`, `note-type`, `lexical-relation`,
  `etymology`…) it fills the existing set in place and *ignores* the `href` (comment line ~541).
  So the corpus' built-in ranges lose their `href` on save.
- **Model.** `FeatureSet.externalContentLoaded` is always `false`, because nothing loads the
  file. `renameFeatureSet` refuses external sets that were not loaded.
- **Writer.**
  - `LiftWriterSession.writeHeaderRange` already writes `id` + `href` only, without content.
  - `writeRangesToExternalFiles` / `writeLiftRangesFile` *rewrite* the ranges file of loaded
    sets. That never happens today, but it is the opposite of what is wanted.
- **Tests.**
  - `RegressionTest.aLoadedExternalRangesFileIsRewrittenWithTheNewId` pins that rewrite.
  - `ChangeRoundTripTest` sets random `href`s on new sets.

## 3. What the corpus shows (DictionaryLift20261022.lift + .lift-ranges)

I checked the new pair with a script.

1. **Header ranges.** 38 header ranges, all `href="DictionaryLift20261022.lift-ranges"` (a
   relative path; an earlier version had the `file://DictionaryLift20261022.lift-ranges`
   form).
2. **Ranges missing from the file.** `dialect`, `location` and `exception-feature` are
   referenced but **not in the file**. FLEx leaves empty ranges out of the file but still lists
   them in the header. With the rule "no matching id → error", **this corpus cannot be
   loaded**. See Q1.
3. **Unreferenced range.** The file has one range the header does not reference
   (`Publications`). It is ignored.
4. **Values used by entries but not declared in their external range:**

   | Range | Undeclared values |
   |---|---|
   | `grammatical-info` | 10 values (`Préposition`×41, `Numéral cardinal`×22, …) |
   | `note-type` | `reference` |
   | `lexical-relation` | `_component-lexeme` (×474, a FLEx-internal type), `Tout` |
   | `inflection-feature` | 11 FLEx feature structures (`{Infl}[absten:pst]`), which are never range elements |

   Today these features are created on the fly: `getOrCreateFeature` in `NoteBuilder`,
   `RelationBuilder`, `EtymologyBuilder`, `ReversalBuilder`, the lenient parse of
   `LiftTrait`, and the `parent` resolution in `endHeader`. With read-only external sets, each
   of these would throw and **the corpus would not load**. See Q2.
5. **The file holds more than label and description:**

   | Element or attribute | Count |
   |---|---|
   | `abbrev` | 2836 |
   | `guid` | 2875 |
   | `parent` (the hierarchy) | 2692 |
   | `trait` | 213 |
   | `field` | 6 |
   | `span` | 5 |

   Annotations are possible too. A "simple" decoder that reads only label and description
   would silently drop all this. Nobody would notice while the set is read-only, but **making
   the set internal would then write the truncated set into the header**: a delayed data loss.
   See §4.1.

## 4. Issues with the request, and suggestions

### 4.1 Reuse the decoder; make the dedicated handler a recorder (recommended)

Instead of a second, simplified decoder, the dedicated SAX handler (`LiftRangesFileReader` +
`LiftRangesSaxHandler`) only **records** the file:

- one small element tree per top-level `<range id>`: element name, attributes copied,
  children, text, and line numbers;
- results kept in a cache.

When the main handler opens `<range id="X" href="F">`, it **replays** the recorded children of
range `X` into itself (its own `startElement` / `characters` / `endElement`), as if they were
written inline.

What this gives:
- one decoder, so traits, fields, annotations, spans, abbrev, guid, `parent` resolution and
  meta-language registration all work as in the header;
- built-in sets are filled in place as today;
- rebinding is already suspended until `endHeader`.

The dedicated handler stays much simpler than the main one. That is the spirit of the request
("a simple version"), without a second copy of the vocabulary that would drift (the writer
already had two copies that drifted; see `writeHeaderRange`'s javadoc).

**Attributes of the recorded `<range>`** (guid, dates, attributes outside the vocabulary) are
applied to the set. The main file's own `<range>` only contributes `id` and `href`.

**Error location.** An error raised during replay names the ranges file and its line, not the
`.lift` file. The recorded nodes carry their line, and the handler's `located(...)` uses them.

### 4.2 Resolving the `href`: try both syntaxes, plus a fallback

The `href` comes from a file written on another machine, so it is resolved in this order:

1. **Path or URI.**
   - If it starts with a URI scheme of two or more letters (`file:`, `http:`; a single letter
     followed by `:` is a Windows drive):
     - `file:` → `Path.of(URI)`. This must also accept `file://name` (authority form, as
       produced by the corpus earlier: treat the authority as the first path segment, relative
       to the `.lift` directory) and `file:///C:/…`.
     - Any other scheme (`http`, `silfw`…) → error. **No network access.**
   - Otherwise it is a path (raw spaces allowed, no percent-decoding), resolved against the
     directory of the `.lift` file. Absolute paths are accepted.
2. **Same name, next to the `.lift` file.** If that file does not exist, try the file of the
   same name (the last segment of the path or URI) in the `.lift` directory. FLEx exports carry
   the exporting machine's absolute path (`file:///C:/Users/…/X.lift-ranges`), which does not
   exist on the reader's machine, while the file usually travels next to the `.lift`.
3. **Otherwise** the file is missing (see Q1).

The cache is keyed by the **resolved real path** (`toRealPath()`), not by the `href` string, so
`X.lift-ranges`, `./X.lift-ranges` and `file:///…/X.lift-ranges` are parsed once. Failures are
cached too, so a missing file is reported once, not 38 times. The cache is cleared at
`endHeader`.

**Security.** The parser is built by the same hardened factory as the main file (no DTD, no
external entities, no XInclude). Move that factory out of `LiftDictionaryXmlReader.parse` into
a shared method. The reader only reads; nothing is written there anymore, so the `../`
truncation guard of `resolveHrefToFile` goes away with the writer code (§5.4).

### 4.3 One source of truth: no separate boolean

`isExternallyDefined()` is derived from `href != null`. A boolean `hasExternalReadOnlyDefinition`
next to the `href` would be two states to keep in step. (The request says "if this property is
turned true, then the href is removed"; I read it as "turned off": making the set internal
removes the `href`.)

`externalContentLoaded` is **deleted**: after this change, an external set is always loaded,
otherwise the load fails (or Q1 applies).

### 4.4 `setHref` must not stay public

Today anyone can call `fs.setHref("x.lift-ranges")` on an internal set (also through
`FeatureSetBuilder.withHref` and `ModelProperty.HREF`). The set would then be written as a bare
reference: its content would vanish on save, and the reload would fail if the file does not
have it. So:

- **Remove the public setter.** `setHref` becomes package-private.
- **One-way public operation:** `LiftHeader.makeFeatureSetInternal(FeatureSet)`. It removes the
  `href` and is reported as a `ModelProperty.HREF` change (old value → `null`).
- **Undo** restores the `href` through the `HREF` applier, by way of `ModelInternals`.
- **Builder.** `FeatureSetBuilder.withHref` moves to `BuilderInternals`, for the reader only.
  An `href` means nothing without the file's content, which only the reader has.

### 4.5 Built-in sets are external too

Four built-ins are external in the corpus: `grammatical-info`, `note-type`, `lexical-relation`
and `etymology`. Their `href` must now be kept (today it is dropped), and they are read-only
like the others. This collides with the builders that create types on the fly (Q2), and with
DictionaryEditor's free typing of note and relation types.

### 4.6 Save As into another directory breaks a relative `href`

A relative `href` written unchanged into a file saved elsewhere points to nothing. Reloading
then fails, and `RoundTripTest` (save in a temp dir, reload) would fail at once. See Q3.

### 4.7 Smaller points

- **Duplicate ids.** A duplicate `<range id>` in the ranges file is an error. Duplicate
  `range-element` ids keep today's behaviour (merged; existing TODO).
- **A range with an `href` and inline content** in the main file: the inline content
  overrides the file (Q5, §4.8).
- **An `href` on a `<range>` inside a ranges file** is an error. The spec (§21) forbids it.
- **The same id twice in the header**, once inline and once as an `href`: an error (today the
  second fills the first in place).
- **Removing an external set from the header** (when unused) stays allowed. It removes the
  reference, not the content of the file.
- **Undo is safe.** `FeatureSetRemoved` revert re-adds a set that already holds its features
  and `href`: it changes no content, so it passes the guard. Edits made after
  `makeFeatureSetInternal` are undone before the `href` comes back (last in, first out).

### 4.8 Inline children override the external file (Q5)

The spec (§21, quoted in LIFT.md) says: *"Any children to this range element in the LIFT file
override the values (by addition or replacement) in the external range definition."* The
user's answer follows it. Choices made here:

- **What overrides what.**
  - An inline `<range-element>` **replaces** the file's element of the same id **as a whole**:
    label, abbrev, description, guid, `parent`, traits, fields and annotations. There is no
    field-by-field merge; partial overrides would need a flag on every slot of a feature.
  - An inline element whose id is not in the file is **added**.
- **The range's own children are covered too.** The spec says "any children":
  - an inline `label`, `description` or `abbrev` of the range replaces the file's one, as a
    whole;
  - inline traits, fields and annotations of the range are added.
- **Flagged as declared inline.**
  - `Feature.isDeclaredInline()`;
  - on the set: which of label, description and abbrev are inline, and which of its own
    traits, fields and annotations are inline.
  - Only the reader sets these flags (package-private, through `ModelInternals`). The
    implicit bare features of Q2 are *not* inline: they are not written, and the reload
    creates them again.
- **Written back.** An external set is written as `<range id href>` followed by its inline
  content only: the inline label, description and abbrev, the inline traits, fields and
  annotations, then the inline range elements sorted by id. Load, save and reload give the
  same file.
- **Inline elements are read-only too, like the rest of the set.** Making them editable
  costs more than it looks:
  - removing an inline element that overrides a file element must bring back the file's
    version (memory = save-and-reload);
  - that means keeping the shadowed element and moving its users (traits, typed components,
    child features' `parent`) to it, a rebinding of features.

  `makeFeatureSetInternal` remains the way to edit, and it makes everything writable at once.
  The flags are kept, so that undoing it puts the set back as it was.
- **Removing an element is not possible.** The spec has no way to remove a file element from
  the `.lift` (LIFT.md asks the same question). The only way is to make the set internal.
- **Hierarchy.** The `parent` attributes are resolved by id at `endHeader`, after every
  override. A file element whose parent is overridden gets the inline object, and an inline
  element may name a file element as its parent.
- **Reader order.**
  - The file's content is replayed when `<range>` opens, then the inline children are read.
  - An inline `<range-element>` whose id the set already has (from the file) **replaces** that
    feature: it is removed (guard suspended, no users yet) and the inline one is built.
  - An inline element declared twice is merged, as today.
  - Inline range-level label, description and abbrev clear the file's forms before being
    filled.

## 5. Design (lift-api)

### 5.1 Reading

- **`xml/LiftRangesFileReader`**
  - `read(Path) → RecordedRangesFile` (`Map<String, RecordedElement>` by range id; duplicate id
    → error; root must be `lift-ranges`).
  - Uses the hardened parser factory.
  - `RecordedElement`: record with `localName`, `Attributes` copy (`AttributesImpl`),
    `List<Object>` children (`RecordedElement` or `String` text), `lineNumber`.
- **`xml/LiftRangesSaxHandler`** builds that tree. It needs no knowledge of LIFT beyond the
  root and `range` levels.
- **`xml/ExternalRangesFiles`** (one per load, owned by the decoder): resolves `href` → `Path`
  (§4.2), keeps the cache `Map<Path, RecordedRangesFile | Failure>`, and is cleared by
  `endHeader()`.
- **`LiftSaxHandler`**, case `range` in the header:
  - open the frame as today;
  - if the attributes have an `href`, ask the decoder for the recorded range (error or Q1 if
    absent) and replay its children through `startElement` / `characters` / `endElement`,
    with the locator switched to the ranges file;
  - the main file's own children of that `<range>` are then read as overrides (§4.8).
- **`LiftXmlDecoder.range`**
  - new set: builder (`href` through `BuilderInternals`);
  - built-in: filled in place, `href` set through `ModelInternals`;
  - the `href` is set **at the end** of the range (the frame's build or end step), so the
    replayed content is written before the set becomes read-only.
- **Missing ranges** (Q1) and the **implicit features** of entries (Q2) per the answers.
- **Inline flags** (§4.8) are set by the decoder on what the main file's `<range>` itself
  contains, never on what the replay brings.

### 5.2 Model: read-only external sets

- **`FeatureSet`**
  - `isExternallyDefined()` (`href != null`);
  - `getHref()` stays;
  - `hrefProperty()` becomes `ReadOnlyObjectProperty`;
  - `setHref` package-private;
  - `isExternalContentLoaded` / `setExternalContentLoaded` deleted.
- **`AbstractLiftRoot.requireWritable()`** (package-private): walks `getParentNode()` up to
  the first `FeatureSet` and throws `IllegalStateException` ("the range X is defined in F;
  make it internal to change it") if that set is external. A single helper, so each mutator
  needs only one line.
- **Where it is called** (Q4 decides the extent; this is the full list):
  - `FeatureSet`: `addFeature`, `removeFeature`, `changeFeatureId`, `setGuid`, dates;
  - `LiftHeader.renameFeatureSet`;
  - `Feature`: `setSuperordinateFeature`, `setGuid`, dates;
  - `MultiText.add` / `removeForm` / annotations, and `Form.changeText` / `append` /
    annotations. `MultiText` knows its parent, so label, abbrev and description of sets and
    features are covered by these two classes;
  - traits, fields and annotations held by sets and features: `addX` / `deleteX` on the
    holder, and the value setters of `LiftTrait` / `LiftField`;
  - `LiftHeader.makeFeatureSetInternal` and the `HREF` applier.
- **Suspension:** a counter on the header, like `rebindingSuspensions`
  (`ModelInternals.withoutReadOnlyGuard(header, Runnable)`). It covers the reader (in case
  content arrives after the `href` is set, e.g. `parent` resolution in `endHeader`), the
  implicit-feature path if Q2 = (a), and the revert/reapply of change records.

### 5.3 Model: making a set internal

`LiftHeader.makeFeatureSetInternal(FeatureSet fs)`:
- refused if `fs` is not in the header or not external;
- `fs.setHref(null)`, reported through `TrackedProperty` (`ModelProperty.HREF`);
- the set keeps its content, and the writer then writes it in full.

### 5.4 Writer

- `writeHeaderRange`, for an external set: `id` and `href` (Q6: nothing else on the element),
  then the inline content only (§4.8). An internal set is written in full, as today.
- **Delete** `writeRangesToExternalFiles`, `writeLiftRangesFile`, `resolveHrefToFile` and the
  call in `write`.
- **Save As** per Q3.

### 5.5 Docs

- `FeatureSet` javadoc;
- `model/package-info` (read-only external sets);
- `LiftHeader` (makeFeatureSetInternal);
- CLAUDE.md core design bullet ("an external set is read-only; reader replays the ranges file;
  writer writes the reference only");
- CLAUDE.md test corpus line (the ranges file is now loaded);
- TODO.md: remove the "built-in ranges lose their href" item.

## 6. Design (DictionaryEditor)

- **`FeatureSetView` / `RangeView`.** When `isExternallyDefined()`:
  - a banner "Defined in `<href>` — read-only";
  - table not editable; Add, Delete and rename disabled; label, description and abbrev editors
    disabled; hierarchy fixed;
  - a column or marker for the elements declared inline in this file (§4.8), as opposed to
    those from the ranges file;
  - a button **"Make editable in this dictionary"** with a confirmation saying the range will
    be saved inside the `.lift` file and will no longer follow the shared file. It calls
    `makeFeatureSetInternal`, is undoable, and the view refreshes on `hrefProperty`.
- **Every place that creates or edits features of a set offers only existing values** when the
  set is external, or offers to make it internal. That covers the note, relation, etymology and
  reversal type pickers with free typing, and the later "new value…" action of `TraitEditor`.
  Also the `MultiTextEditor` instances on a feature's texts. Q2 affects this.
- **`DictionaryValidator`**:
  - values not declared in their external range (if Q2 = (a));
  - ranges referenced but missing from the file (if Q1 = (b)).
- **`ChangeLabels`**: a label for the `HREF` change on a set ("Make range X editable").
- **Messages** in en and fr.
- **Defensive catch.** The `IllegalStateException` thrown by the model is caught where an edit
  could still slip through (like `cfg.rangeChangeNotAllowed`).

## 7. Tests

**lift-api:**
- **`ExternalRangesTest` (new):**
  - loading the corpus fills every external set from the file (labels, abbrev, guid,
    hierarchy, traits);
  - the file is parsed once (a counter or a spy on `LiftRangesFileReader`);
  - relative path, `file:///abs`, `file://name`, absolute path from another machine falling
    back to the `.lift` directory, `http:` refused;
  - missing id (Q1);
  - duplicate id in the file;
  - inline overrides (§4.8): an inline element replaces the file's one as a whole, an
    inline element with a new id is added, inline range label replaces the file's, inline
    elements are read-only, load → save → reload keeps exactly the inline content, parent
    links across inline and file elements, `href` inside the ranges file refused;
  - error message names the ranges file and line;
  - every guarded mutator throws on an external set, and works after
    `makeFeatureSetInternal`;
  - save writes the reference only and does not touch the ranges file;
  - after `makeFeatureSetInternal`, save writes the full set and drops the `href`;
  - undo of `makeFeatureSetInternal`;
  - Save As into another directory (Q3);
  - built-in external sets keep their `href`.
- **`RegressionTest`:**
  - replace `aLoadedExternalRangesFileIsRewrittenWithTheNewId` with "an external set cannot be
    renamed";
  - add "the built-in ranges keep their href on save".
- **`LiftDictionaryWriterTest.saveDoesNotRewriteExternalRangesThatWereNeverRead`** becomes
  "save never writes a ranges file".
- **`ChangeRoundTripTest`:** no more random `setHref`; add the op "make a set internal" (the
  corpus provides external sets).
- **`DictionarySnapshot` / `ModelSnapshot`:** include `href`, `isExternallyDefined` and the
  inline flags.
- **`DictionaryCensusTest`, `RoundTripTest`:** counts and comparisons change once the file is
  loaded. `RoundTripTest` needs Q3.

**DictionaryEditor:** view tests for disabled editing and the button, validator tests,
`ComponentTablesTest` if it uses `href`.

## 8. Steps

1. Hardened parser factory shared; `LiftRangesSaxHandler`, `LiftRangesFileReader`,
   `ExternalRangesFiles` (resolution and cache) with unit tests on small files.
2. Replay in `LiftSaxHandler`; `href` kept on new and built-in sets; Q1; inline overrides and
   their flags (§4.8). Corpus loads with Q1/Q2 answers applied.
3. Model: `isExternallyDefined`, `requireWritable`, guards, suspension; `setHref` restricted;
   `makeFeatureSetInternal`; delete `externalContentLoaded`.
4. Writer: delete the ranges-file writing; inline content of external sets; Save As (Q3).
5. Tests and docs in lift-api; `mvn clean test`; `mvn install`.
6. DictionaryEditor: views, button, pickers, validator, labels, messages, tests.

## 9. Questions (suggested answers first)

- **Q1 — A referenced range missing from the file** (the corpus has 3: `dialect`, `location`,
  `exception-feature`).
  - (a) error, as requested: the corpus cannot be loaded;
  - **(b) suggested: an empty external range with a warning.** That is what FLEx means: it
    leaves empty ranges out of the file. It stays read-only and is written back as the same
    reference. Error only in strict mode (the reader's `strict` flag).
- **Q2 — Values used by entries that the external range does not declare** (corpus:
  `grammatical-info`, `note-type`, `lexical-relation`, `inflection-feature`).
  - **(a) suggested:** creating such a *bare* feature (id only) is allowed on an external set,
    for the reader and the builders only (through `ModelInternals`, not the public
    `addFeature` / `getOrCreateFeature`, which refuse). It is not an edit of the definition:
    the reload recreates it the same way, so memory still equals save-and-reload. The validator
    reports these values.
  - (b) refuse: these files cannot be loaded.
  - (c) make the set internal automatically: this rewrites a shared definition behind the
    user's back.
- **Q3 — Save As into another directory with a relative `href`.**
  - **(a) suggested:** write the `href` so it still points to the same file from the new
    location: relative if possible, otherwise a `file:` URI. The set remembers the resolved
    path of its file (transient, not data). Nothing is copied or overwritten.
  - (b) copy the ranges file next to the new `.lift` if no file of that name is there.
  - (c) write it unchanged (broken reference).
- **Q4 — Extent of the guard.**
  - **(a) suggested: everything listed in §5.2**, in the model, so that what is in memory is
    what the file says.
  - (b) structural operations only (id, features, hierarchy, `href`), with text edits blocked
    by DictionaryEditor alone.
- **Q5 — `<range href>` with inline content in the main file.** **Answered by the user:**
  inline children override the file, are flagged as declared inline, and are written back.
  This matches the spec (§21). Details in §4.8.
- **Q6 — The reference element.** Write only `id` + `href` (**suggested**, as requested), or
  also the `guid` if the main file had one on the reference?

## 10. Implementation notes (differences from the plan)

- **One reader class, no separate handler class.** The recorder is a nested SAX handler of
  `xml/LiftRangesFileReader` (no `LiftRangesSaxHandler` file). `RecordedElement` and
  `ExternalRangesFiles` are as planned; the hardened factory moved to `xml/LiftXmlParsers`.
- **The guard** is `model/ExternalDefinitionGuard` (`requireWritable`, suspended per thread).
  Besides the listed mutators, it is called by `Tracked*Property.set` (so bindings are
  checked) and by `AbstractLiftRoot.detach`. The `HREF` property is not guarded.
  `TextSpan` setters are not guarded (spans are only built by the reader).
- **Change records** run `revert`/`reapply` without the guard (all of them, through
  `ModelInternals.withoutExternalDefinitionGuard`), not only the ones that touch sets.
- **Builders**: the type lookups of all builders go through
  `AbstractLiftElementBuilder.typeFeature` → `ModelInternals.getOrCreateUndeclaredFeature`
  (notes, relations, etymologies, reversals, translations, annotations, parts of speech).
  `FeatureSetBuilder.withHref` is removed (not moved to `BuilderInternals`: the reader sets
  the `href` through `ModelInternals` when the `<range>` closes).
- **Duplicate range elements** (found in the corpus' ranges file: `non` three times, FLEx
  cuts ids at the first space): the first declaration wins, the others are skipped with a
  warning. Previously they were merged, which failed on a duplicate language. A bare
  feature created by a value before its declaration is still filled in place.
- **Two facts added for the validator**: `Feature.isUndeclaredInTheExternalFile()` (the bare
  features of Q2) and `FeatureSet.isExternalDefinitionMissing()` (Q1).
- **Q3**: the `href` read is kept if it names the same file from the new directory *without*
  the fallback on the file name; otherwise a relative path, or a `file:` URI.
- **Attributes on the reference** (`guid`, dates on the `.lift` file's `<range href>`) are
  applied after the file's, but not written back (Q6).
- **DictionaryEditor**: the type pickers needed no change (they offer existing features; new
  types go through the builders, Q2). The demo dictionary's ranges are external, so the UI
  tests that edit them make them internal first.
- `TODO.md` was left untouched (the user's working notes).
