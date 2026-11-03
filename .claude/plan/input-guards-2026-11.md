# Plan: guarding the input document, and reporting what the reader drops

Status: implemented 2026-11-03 (steps 1-5, §5, §7; step 6 waits for a release). Decisions taken:

- `getLoadWarnings()` is **replaced** by `getLoadDiagnostics()` (option A).
  *Correction (implementation):* `getLoadWarnings()` **is** in the released 0.1.0 (the first
  check of the tag was wrong). So it is kept one release as a `@Deprecated(forRemoval = true)`
  view (the messages of the diagnostics), and DictionaryEditor, which builds against 0.1.0
  from Central by default, moves to `getLoadDiagnostics()` once a release has it (step 6).
- Strict mode refuses **what would be lost on save** only, not every warning.

## 1. Findings

Reproduced with the command-line jar:

| Document | Today |
|---|---|
| `<html><body>…</body></html>` | loads, 0 entries; one log line `Skipping unknown LIFT element <html>` |
| `<html><entry>…</entry></html>` | loads, 0 entries: the root is skipped with its whole subtree |
| `<x:lift xmlns:x="urn:other"><x:entry/></x:lift>` | read as LIFT, 1 entry: the namespace URI is never looked at |
| `<lift>` nested in an `<entry>` | accepted; `startLift` runs again and overwrites version and producer |
| `<foo><entry/></foo>` under `<lift>` | `<foo>` and its entry skipped, logged only |
| `<lift version="0.15">` without `producer` | **load fails** (`liftProducer cannot be null`), though `producer` is optional in both schemas |

Causes, in `xml/LiftSaxHandler`:

- `open()` switches on `localName` at any depth: `lift` is one case among others, and no
  root check exists (unlike `LiftRangesFileReader`, which refuses a root other than
  `<lift-ranges>`).
- `startElement` ignores `uri`; LIFT elements are in no namespace (`LiftVocabulary.LIFT_URI = ""`).
- `skipUnknownElement` only logs. The same holds for unknown attributes
  (`LiftXmlDecoder.other`), recoverable XML errors (`LiftSaxHandler.error`), a definition
  declared twice, a definition of the wrong kind (`warnIfTheDefinitionIsForTheOtherKind`):
  none reaches `getLoadWarnings()`, which only holds the warnings about definitions.
- `<grammatical-info>` in `<reversal>` / `<main>` is **valid LIFT** (both schemas) that the
  model does not hold: it is skipped as "unknown", and lost on save.
- `loadDictionaryFromFile` hard-codes `strict = false`: no caller can ask for a strict load.

The element names of `LiftVocabulary` match the 0.13 and 0.15 schemas exactly, and the
schemas have no wildcard (`anyName`): extensions go through `<field>` and `<trait>`. An
unknown element is therefore an error in the file, not an extension of LIFT.

## 2. Design principles

1. **Closed vocabulary: report foreign content, do not keep it.** Preserving unknown XML
   verbatim (the `RecordedElement` capture could do it) would make the model carry content it
   cannot edit, place, or check, for a format that has its own extension mechanism. Not done.
2. **Fail fast on the wrong document, tolerate a damaged one.** A wrong root fails at the first
   start tag, before anything is built. Problems inside a LIFT document are recorded and the
   load goes on, unless the caller asks for a strict load.
3. **One structured channel** for everything the reader notices, on the model of compiler
   diagnostics (`javax.tools.Diagnostic`: kind, severity, position, message), and of
   `export.LatexExportWarning` in this library (kind enum + message + count).
4. **No cost on a clean file.** The normal path gains one `boolean` test and one
   `uri.isEmpty()` per element; everything else runs only when something is wrong.

## 3. Steps

Each step is one commit, with its tests.

### Step 1: root and namespace guard (`xml/LiftSaxHandler`)

- A `rootSeen` flag. At the first `startElement`, anything but `lift` in no namespace throws
  `NotALiftDocumentException` (new, root package, `extends LiftDocumentLoadingException`;
  carried out of SAX like `UnsupportedVersionException`, and unwrapped by
  `LiftDictionaryXmlReader.parse`). The parser stops there: a large non-LIFT file is refused
  after its first bytes. Messages:
  - root `<lift-ranges>`: "this is a ranges file; open the .lift file that refers to it";
  - `lift` in a namespace: "the root element <lift> is in the namespace X; LIFT elements are in no namespace";
  - otherwise: "the root element is <html>, not <lift>: this is not a LIFT document".
- After the root, a `lift` element is an unknown element (skipped, reported): it can no
  longer reset the version.
- An element with a non-empty `uri` is an unknown element, even with a LIFT local name. The
  replay of external ranges passes `LIFT_URI`, so it is unaffected.
- Fix `LiftXmlDecoder.startLift`: no `producer` keeps the dictionary's default producer
  instead of calling `setLiftProducer(null)`. A case in `RegressionTest`.
- Remove the TODO.md item "It should also be fixed to refuse a root other than <lift>."

### Step 2: `LoadDiagnostic` (exported, root package)

```java
/// Something the reader noticed while loading a file.
public record LoadDiagnostic(
    Kind kind, Severity severity, String message,
    Path file,            // the .lift, or the .lift-ranges being replayed
    int line, int column, // of the first occurrence; -1 if unknown
    String subject,       // element or attribute name, range or value id...
    int occurrences) {

    public enum Severity { INFO, WARNING, ERROR }

    public enum Kind {
        UNKNOWN_ELEMENT,          // not LIFT: skipped with its subtree
        UNSUPPORTED_ELEMENT,      // valid LIFT the model does not hold (until §5)
        UNKNOWN_ATTRIBUTE,        // kept in getOtherXmlAttributes(), not saved
        XML_ERROR,                // recoverable error of the parser
        MISSING_EXTERNAL_RANGE,
        DUPLICATE_RANGE_ELEMENT,
        DUPLICATE_DEFINITION,
        DEFINITION_KIND_MISMATCH,
        UNDECLARED_RANGE,         // kept, round-trips
        UNDECLARED_VALUE;         // kept, round-trips

        /// Whether saving the dictionary leaves out what this diagnostic is about.
        public boolean losesDataOnSave() { … }
    }
}
```

- Severity follows from the kind: what is lost on save is a `WARNING`; what round-trips
  (`UNDECLARED_RANGE`, `UNDECLARED_VALUE`) is `INFO`; `XML_ERROR` is an `ERROR`.
- `LiftDictionary.getLoadDiagnostics()`: read-only list, in order of first occurrence; empty
  for a dictionary not read from a file. `LiftDictionary.losesDataOnSave()`: whether any
  diagnostic `losesDataOnSave()`, for a GUI to warn before `save()` overwrites the source.
- `getLoadWarnings()` is removed; `UndeclaredValuesTest` checks `kind()` and `subject()`
  instead of searching message text.

### Step 3: one collector, aggregated (`xml` package)

- `LoadDiagnostics` (package-private), owned by `LiftXmlDecoder`, reached by
  `LiftSaxHandler` through it. It replaces `warn()`, the scattered `LOGGER.warning` calls, and
  the sets `undeclaredRangesWarned`, `undeclaredValuesWarned`,
  `namesWarnedAboutTheirDefinitionKind`.
- Keyed by `(kind, subject)`: the first occurrence records its position and is logged once;
  the next ones increment `occurrences`. A `<custom>` in each of 2,600 entries is one
  diagnostic with `occurrences = 2600`, not 2,600 log lines.
- An unknown element is counted at the root of the skipped subtree, not for its descendants
  (`skipDepth` already gives that).
- At `endDocument` the list is handed to the dictionary; `DictionaryMutator.recordLoadWarning`
  / `loadWarnings` go (reader state does not belong to the mutation core).
- Diagnostics raised while an external range is replayed name the `.lift-ranges` file and its
  line (`replayedFile` and the replay locator already carry them).
- Migration order inside the step: unknown and unsupported elements; unknown attributes;
  XML errors; then the existing warnings about definitions.

### Step 4: load options

```java
LiftDictionary.loadDictionaryFromFile(File f)                     // LoadOptions.defaults()
LiftDictionary.loadDictionaryFromFile(File f, LoadOptions options)

public record LoadOptions(boolean strict, int maxDiagnostics) {
    public static LoadOptions defaults() { return new LoadOptions(false, 1000); }
    public LoadOptions withStrict(boolean strict) { … }
}
```

- `strict`: the first diagnostic whose kind `losesDataOnSave()` fails the load, with its
  position, as do an `XML_ERROR` and a `MISSING_EXTERNAL_RANGE` (strict already makes them
  fatal). `INFO` diagnostics never fail a load: FLEx exports have many undeclared values, and
  they round-trip.
- `maxDiagnostics`: distinct diagnostics kept; past it, occurrences are still counted in a
  final "N more problems" entry, so a pathological file cannot grow the list without bound.
- A record leaves room for later options (e.g. schema validation, §6) without new overloads.
  `LiftDictionaryXmlReader` takes the options instead of its `strict` boolean.

### Step 5: command line (`cli/InfoCommand`, `cli/Main`)

- `info` prints the diagnostics after the summary, one line each:
  `warning: line 12: unknown element <custom> (2600 times); it will not be saved`.
- `--strict` loads with `LoadOptions.withStrict(true)`.
- Exit codes, documented in the command's description as `latex` does: 0 read; 1 cannot be
  read (including not a LIFT document, and a strict failure); 2 bad command line;
  3 read, but saving would lose part of the file.
- `Main.load` takes the options.

### Step 6: DictionaryEditor (sibling project)

`FileActions.openFile` uses `getLoadDiagnostics()`: the diagnostics that lose data first,
under their own header ("will not be saved"); the `INFO` ones under the present header (kept,
read-only, not saved in the header). Messages built from `kind()` and `subject()` through
`I18n`, so they are translated. Not part of this repository: a separate commit there.

## 4. Tests

- `xml/InputGuardTest` (new):
  - refused, with line and reason: a root `<html>`, a root `<lift-ranges>`, `<lift>` in a namespace;
  - a `<lift>` nested in an entry, and an `<entry>` in a namespace: `UNKNOWN_ELEMENT`, version unchanged;
  - an unknown element 100 times: one diagnostic, `occurrences == 100`, line of the first;
  - an unknown element inside a range of a `.lift-ranges` file: the diagnostic names that file;
  - `grammatical-info` in a `reversal`: `UNSUPPORTED_ELEMENT` (until §5 removes the case);
  - strict mode: an unknown element or attribute fails the load; an undeclared value does not;
  - `maxDiagnostics` reached: the overflow entry counts the rest.
- `RegressionTest`: `<lift>` without `producer` loads.
- `UndeclaredValuesTest`, `ExternalRangesTest`: moved to `getLoadDiagnostics()`.
- Corpus (`DictionaryLift20261022.lift`): no diagnostic of severity `WARNING` or `ERROR`;
  pins "a clean file gives no noise".
- `cli/MainTest`: `info` on a file with an unknown element prints it and exits 3; on a
  non-LIFT document exits 1; `--strict` exits 1.
- Performance: `LoadingCostTest` before and after (`mvn test -Dgroups=perf
  -DexcludedTestGroups=none -Dtest=LoadingCostTest`); no measurable difference expected.

## 5. Follow-up: modelling `grammatical-info` in `reversal`

Both schemas allow an optional `<grammatical-info>` (a `value` plus traits) in `<reversal>` and
in its `<main>` (reversal-content and reversal-main). The model only gives it to `LiftSense`,
so it is the one `UNSUPPORTED_ELEMENT` case; once modelled, the kind can be kept for future
schema versions or removed.

Design: `GrammaticalInfo` gets a parent of an interface type instead of `LiftSense`.

- `model/HasGrammaticalInfo` (new): `getGrammaticalInfo()`, `grammaticalInfoProperty()`,
  `setGrammaticalInfo(GrammaticalInfo)`, `deleteGrammaticalInfo()`; implemented by `LiftSense`
  and `LiftReversal` (which also stands for `<main>`). The single-slot storage of `LiftSense`
  (`storeGrammaticalInfo`, lazy property) moves into a small helper both use, or is repeated.
- `GrammaticalInfo.getParent()` returns `HasGrammaticalInfo`; `getParentNode()` and
  `AbstractLiftRoot.detach()` (`clearGrammaticalInfo`) follow.

Then the checklist of CLAUDE.md ("adding/changing a component kind or a child slot"), for the
new slot:

1. `model/AbstractLiftRoot`: `detach()` case for a reversal parent.
2. `internal/DictionaryMutator`: `childrenOf` (a reversal's grammatical info), `wire`
   (`HasGrammaticalInfo` instead of the `LiftSense` cast). The traits of the grammatical info
   of a reversal are then registered like those of a sense.
3. `internal/DictionaryRegisters`, `LiftDictionaryRegistry`: grammatical infos are already
   indexed by kind; check that one under a reversal is.
4. Builder: `GrammaticalInfoBuilder` and `DictionaryComponentBuilderFactory.grammaticalInfo`
   take a `HasGrammaticalInfo`; `ReversalBuilder.withGrammaticalInfo(...)`, as `SenseBuilder`
   has.
5. Reader: `LiftSaxHandler.open` builds it when `top()` is a `LiftSense` or a `LiftReversal`
   (the `skipUnknownElement("grammatical-info outside a sense")` branch goes);
   `LiftXmlDecoder.grammaticalInfo` drops its `(LiftSense)` cast.
6. Writer: `LiftWriterSession` writes it in `<reversal>` and `<main>`, after the forms and the
   nested `<main>` (the schema's order is free: `interleave`).
7. Header: the part-of-speech feature it names counts as a use (`LiftHeader.isUsed` /
   `countUses`, the part-of-speech counter of `LiftDictionaryCounterManager`), so a part of
   speech used only by reversals cannot be removed.
8. Change reporting: `change/Slots` puts it back in a reversal; `ModelProperty`
   `grammaticalInfoValue` already applies to any `GrammaticalInfo`.
9. Tests: `DictionaryCensusTest`, `xml/RoundTripTest` + `DictionarySnapshot` (a reversal with
   grammatical info, at both levels), `AttachmentTest`/`DeletionTest`,
   `change/ChangeRoundTripTest` + `ModelSnapshot`, `EncapsulationTest`.

## 6. Out of scope

- **Schema validation** (RELAX NG through Jing; the XSD exists for 0.13 only), as a later
  `LoadOptions.validate`: it would catch LIFT elements in the wrong place, which a vocabulary
  check cannot. A dependency and roughly a second pass over the file: off by default.
- **Keeping foreign XML** for the round trip: not wanted for a closed vocabulary (§2.1).

## 7. Documentation

- **README.md, section "CLI"**: document the `info` command. Today the section shows the bare
  `java -jar … <dictionaryFile>` and only names `info` in passing. Add:
  - `lift-api info <dictionaryFile>` (and that `lift-api <dictionaryFile>` is the same): what
    it prints (entries, senses, examples, languages with their counts, then the load
    diagnostics), with a short sample output;
  - `--strict`, and the exit codes 0 / 1 / 2 / 3;
  - that a document whose root is not `<lift>` is refused.
  The section "As a command-line tool" links to it; check that its example still matches.
- **README.md, loading**: a short example of `loadDictionaryFromFile(f, LoadOptions…)` and of
  reading `getLoadDiagnostics()` / `losesDataOnSave()`.
- **Javadoc**: `LiftDictionary.loadDictionaryFromFile` (what is refused, what is reported,
  strict mode), `LoadDiagnostic` and each `Kind`, `LoadOptions`; the class javadocs of
  `LiftSaxHandler` (root and namespace rule, unknown elements) and `LiftXmlDecoder` (unknown
  attributes now reported as diagnostics).
- **CLAUDE.md**: in "Core design", one bullet: the root must be `<lift>` in no namespace;
  everything the reader skips or does not save goes through `LoadDiagnostics`, never only
  the logger. In the package map, `LoadDiagnostic` / `LoadOptions` in the root package.
- **TODO.md**: remove the item about the root element; add §5 if it is not done with this plan.
