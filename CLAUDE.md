# lift-api — notes for Claude

Java library that models LIFT (Lexicon Interchange FormaT) dictionaries: load/save LIFT XML
(v0.13 and v0.15), walk and query the model, and mutate it. It is one module of the Maven
multi-module project in `..` (`multilingual-dictionary-editor`); the sibling `../lift-gui`
is a JavaFX editor that binds directly to this model.

## Build & test

- Java 25 (`maven.compiler.release=25`), JPMS module `fr.cnrs.lacito.liftapi`.
- From this directory: `mvn test`; one test: `mvn test -Dtest=MultiTextTest#testTextAndSeveralSpan`.
  From the repo root: `mvn test -pl lift-api`; `mvn install` installs both modules.
- ~120 JUnit 6 tests, all fast (<5 s). They are headless — no JavaFX toolkit is started.
- `mvn package` also builds `target/lift-api-0.1-SNAPSHOT-jar-with-dependencies.jar`;
  its main class `cli.Main` (picocli) loads a file and prints a summary or the error.
- Test logs go to `target/lift-api-test*.log` (see `src/test/resources/logging.properties`).
- The IDE's Eclipse compiler also writes into `target/classes`, so Maven may consider
  classes up to date and skip recompiling, even when they contain "Unresolved compilation
  problem" stubs. If a build looks suspiciously clean, `rm -rf target/classes target/test-classes`
  first.

## Package map (`src/main/java/fr/cnrs/lacito/liftapi/`)

| Package | Exported | Role |
|---|---|---|
| (root) | yes | `LiftDictionary` (entry point: load/save/addEntry/removeEntry/search), `LiftDictionaryRegistry` (read-only observable views of all components), `LiftDictionaryLanguagesManager` (object- vs meta-language sets + occurrence counts), `LiftDictionaryCounterManager`, `LiftDictionaryBuilder`, `LiftVersion` |
| `model` | yes | The LIFT data model (~50 classes). `Lift*` component classes, `Has*` capability interfaces, `MultiText`/`Form`/`TextSpan` text model, `FeatureSet`/`Feature` (= LIFT `range`/`range-element`), `LiftHeader`, `LiftFieldAndTraitDefinition` |
| `builder` | yes | Fluent API, reached via `dictionary.getComponentBuilder()` (`DictionaryComponentBuilderFactory`) |
| `internal` | **no** | `DictionaryMutator` (the only code that writes indexes), `DictionaryRegisters` (the index maps), UUID manager |
| `xml` | **no** | SAX reader (`LiftDictionaryXmlReader` → `LiftSaxHandler` → `LiftXMLFactory`), writer (`LiftWriterSession`, `*Writers`), `LiftVocabulary` (element/attribute names) |
| `cli` | no (opened to picocli) | `Main` |

`internal`/`xml` types are `public` only for cross-package access inside the module; the
non-export in `module-info.java` is what keeps them private. Keep it that way.

Resources: LIFT schemas in `src/main/resources/fr/cnrs/lacito/liftapi/schema/`.
Test corpora in `src/test/resources/lift/` — `20240828Lift.lift` is a real ~1900-entry
dictionary (with a `.lift-ranges` file) used by the census and round-trip tests.

## Core design (read before changing mutation code)

The authoritative docs are the javadocs of `model/package-info.java`,
`package-info.java` (threading + JavaFX rationale) and `internal/DictionaryMutator.java`.
Summary:

- **Two linkages** must stay consistent: parent⇄child references in the tree, and the
  per-kind UUID-keyed indexes in `DictionaryRegisters` (`entriesById`, `sensesById`, …,
  `objectTextById`, `metaTextById`, plus `entries` list in document order and
  `entriesByLiftId`/`sensesByLiftId`).
- **Attached vs detached**: a component is attached iff walking `getParentNode()` reaches a
  `LiftEntry` whose owning dictionary is set (`AbstractLiftRoot.getOwningDictionary()`).
- **The one rule**: `parent.addX(child)` registers the child subtree, and
  `parent.deleteX(child)` unregisters it, *iff the parent is attached* (via
  `AbstractLiftRoot.adopted()` / `orphaned()` → `DictionaryMutator.adoptSubtree` /
  `releaseSubtree`). Entries have no parent: use `LiftDictionary.addEntry/removeEntry`.
- `detach()` unlinks but **keeps** registration (for moves/undo); `deleteX` unlinks
  **and** unregisters (subtree comes back UUID-free, reusable in another dictionary).
- Builders call `DictionaryMutator.attach()` (register first, then wire). The XML reader
  instead assembles a whole detached `<entry>` and adopts it once (`addEntryToDictionary`).
- Adoption is idempotent for nodes already registered in the same dictionary; a node with a
  UUID from another dictionary is rejected.
- Deletion is refused (`IllegalStateException`) while another component references the
  target's LIFT id (ref counting on `HasRefId`: relations, variants).
- `MultiText` has an optional language manager: once registered, adding a `Form` in a
  language the dictionary doesn't declare throws. Registration of a pre-filled MultiText
  (parse, moved subtree) declares its languages instead. Each MultiText is either
  object-language or meta-language — decided by `objectTextsOf`/`metaTextsOf`.
- **Types (`HasType`) change only through `setType`**, which delegates to the parent's
  `retypeX` (`retypeNote`, `retypeTranslation`, `retypeRelation`, …) when there is a
  parent. `typeProperty()` is read-only. Parents that key children by type (notes,
  translations) refuse duplicates there and re-key. Anything keyed by a type uses the
  `Feature` object, never its id: `Feature` compares by identity, so
  `FeatureSet.changeFeatureId` renames without invalidating keys.

### Checklist: adding/changing a component kind or a child slot

A missing case here silently drops components on load and leaves them behind on delete
(this has happened — see `DictionaryCensusTest`). Touch all of:

1. `model/AbstractLiftRoot` — `permits` list and `detach()` switch.
   Model it as an `AbstractLiftRoot` child with `addX`/`deleteX` on its parent
   (see `LiftTranslation` for a minimal one), never as a bare `MultiText` in a map:
   a bare `MultiText` created after attachment escapes registration. If it is typed,
   implement `HasType` with a read-only `typeProperty`, a package-private `assignType`,
   and a `retypeX` on its parent.
2. `internal/DictionaryMutator` — `childrenOf`, `objectTextsOf`/`metaTextsOf`, `wire`.
3. `internal/DictionaryRegisters` — the index map and `nodesById`.
4. `LiftDictionaryRegistry` — public read-only getter.
5. `xml/LiftSaxHandler` + `xml/LiftXMLFactory` (read), `xml/LiftWriterSession` (write),
   `xml/LiftVocabulary` (names); watch v0.13 vs v0.15 spelling differences.
6. Builder in `builder/` + factory method, if it should be creatable fluently.
7. Tests: `DictionaryCensusTest` (every element in the corpus is registered),
   `xml/RoundTripTest` + `xml/DictionarySnapshot` (load→save→load equality),
   `AttachmentTest`/`DeletionTest` (the add/delete contract), `RetypeTest`.

## Conventions & deliberate choices

- **JavaFX properties/observable collections in the public API are intentional** (GUI
  binding; `requires transitive javafx.base`). Do not flag or refactor them away.
- Not thread-safe; a dictionary is confined to one thread (FX thread once a UI observes it).
- Javadoc is thorough and explains *why*; match that style (both `/** */` and `///`
  Markdown javadoc are used). Commit messages are descriptive sentences.
- Save is atomic: `LiftWriterSession` writes a temp file and promotes it only on success.
- `RegressionTest` pins bugs found in earlier audits — add a case there for new bug fixes.
- `TODO.md` lists known open issues (e.g. form-level `<annotation>` inside `<form>` is
  never registered).
- `src/main/xbj/ChoiceBinding.xjb` is a leftover JAXB binding, not used by the build.
