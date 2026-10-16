# lift-api — notes for Claude

Java library that models LIFT (Lexicon Interchange FormaT) dictionaries: load/save LIFT XML
(v0.13 and v0.15), walk and query the model, and mutate it. It is one module of the Maven
multi-module project in `..` (`multilingual-dictionary-editor`); the sibling `../lift-gui`
is a JavaFX editor that binds directly to this model.

## Build & test

- Java 25 (`maven.compiler.release=25`), JPMS module `fr.cnrs.lacito.liftapi`.
- From this directory: `mvn test`; one test: `mvn test -Dtest=MultiTextTest#testTextAndSeveralSpan`.
  From the repo root: `mvn test -pl lift-api`; `mvn install` installs both modules.
- ~190 JUnit 6 tests, all fast (~20 s, most of it `ChangeRoundTripTest`). They are headless —
  no JavaFX toolkit is started.
- `LoadingCostTest` (tag `perf`) measures the time and heap of loading the corpus; it is
  excluded by default: `mvn test -Dgroups=perf -DexcludedTestGroups=none -Dtest=LoadingCostTest`.
- `mvn package` also builds `target/lift-api-0.1-SNAPSHOT-jar-with-dependencies.jar`;
  its main class `cli.Main` (picocli) loads a file and prints a summary or the error.
- Test logs go to `target/lift-api-test*.log` (see `src/test/resources/logging.properties`).
- The IDE's Eclipse compiler also writes into `target/classes`, so Maven may consider
  classes up to date and skip recompiling, even when they contain "Unresolved compilation
  problem" stubs. Use `mvn clean test` (a `rm -rf target/...` followed by `mvn test` can lose
  the race against the IDE).

## Package map (`src/main/java/fr/cnrs/lacito/liftapi/`)

| Package | Exported | Role |
|---|---|---|
| (root) | yes | `LiftDictionary` (entry point: load/save/addEntry/removeEntry/search), `LiftDictionaryRegistry` (read-only observable views of all components), `LiftDictionaryLanguagesManager` (object- vs meta-language sets + occurrence counts), `LiftDictionaryCounterManager`, `LiftDictionaryBuilder`, `LiftVersion` |
| `change` | yes | The changes of a dictionary as reversible events: `ModelChange` (sealed) and its records, `ModelProperty`, `ChangeRecorder` (transactions). Read its `package-info` |
| `model` | yes | The LIFT data model (~50 classes). `Lift*` component classes, `Has*` capability interfaces, `MultiText`/`Form`/`TextSpan` text model, `FeatureSet`/`Feature` (= LIFT `range`/`range-element`), `LiftHeader`, `LiftFieldAndTraitDefinition` |
| `builder` | yes | Fluent API, reached via `dictionary.getComponentBuilder()` (`DictionaryComponentBuilderFactory`). The only code that creates components (the XML reader included): builders accept what the LIFT schema accepts, no editorial checks. Read its `package-info` |
| `internal` | **no** | `DictionaryMutator` (the only code that writes indexes), `DictionaryRegisters` (the index maps), `ChangeSupport` (listeners), `ModelInternals` (the model's package-private wiring: `setUUID`, `setOwningDictionary`…), `BuilderInternals` (what only the XML reader may do with a builder: reach the element under construction), `LiftDictionaryUUIDManager` (internal UUIDs: per-dictionary prefix + counter, not random — never write them to a file; default LIFT ids are separate random UUIDs) |
| `xml` | **no** | SAX reader (`LiftDictionaryXmlReader` → `LiftSaxHandler`, the document structure → `LiftXmlDecoder`, elements and attributes turned into builder calls), writer (`LiftWriterSession`, `*Writers`), `LiftVocabulary` (element/attribute names) |
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
- Builders call `DictionaryMutator.attach()`, which wires the child with the parent's
  `addX` (so registration and reporting take the same path as any caller). Nested
  builders build into a detached parent, so a subtree is registered once, when its
  outermost component is attached. The XML reader is a builder client like any other:
  it keeps the builders of the open elements on a stack, builds each at its end tag,
  and the `<entry>`'s `build()` registers the whole entry.
- References (`ref` of relations and variants) may name an entry or sense that does not
  exist yet: the id is recorded and counted, and `LiftDictionary.resolveReferences()`
  (called at the end of a load) sets the target objects.
- The header's components (feature sets, features, definitions) are not indexed and
  have no UUID, but their MultiTexts are registered as meta-language texts when they
  join the header (`DictionaryMutator.registerHeaderTexts`): languages declared and
  counted, undeclared ones refused.
- Adoption and release are all-or-nothing: a failure part-way (a duplicate id, a
  referenced component) leaves the dictionary as it was, and `addX` unwires the child.
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
- **Change events** (`change` package): `AbstractLiftRoot.adopted`/`removeChild`/`detach`
  report components added and removed; values report through `TrackedProperty` (and its
  String/Integer variants), which override `invalidated()`, or through the setter when the
  property has not been created yet (lazy properties: a plain field until `xxxProperty()`
  is called). Every emission point starts with `Changes.observing(owner)`, whose first test
  is a global "anyone listening?" counter: loading costs nothing more.
- Collections handed out are read-only views; the mutable ones are package-private fields
  (`senses`, `traits`, `notes`…) used by the `addX`/`deleteX` of the class and by
  `AbstractLiftRoot.unlink`/`positionInParent`.

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
5. Builder in `builder/` + its factory method in `DictionaryComponentBuilderFactory`
   (every component is created through a builder, the XML reader included).
6. `xml/LiftSaxHandler` (element structure, MultiText to fill) + `xml/LiftXmlDecoder`
   (which builder, attribute mapping) for reading, `xml/LiftWriterSession` (write),
   `xml/LiftVocabulary` (names); watch v0.13 vs v0.15 spelling differences.
7. Tests: `DictionaryCensusTest` (every element in the corpus is registered),
   `xml/RoundTripTest` + `xml/DictionarySnapshot` (load→save→load equality),
   `AttachmentTest`/`DeletionTest` (the add/delete contract), `RetypeTest`.
8. Encapsulation: a read-only view as getter (`FXCollections.unmodifiable…`, created on the
   first call), the mutable collection a package-private field, mutated only by
   `addX`/`deleteX` (`EncapsulationTest` checks it by reflection).
9. For a list-held component, `addX(int, X)` (and in its `Has*` interface), and its case in
   `AbstractLiftRoot.positionInParent()`; `change/Slots` (how an event puts it back).
10. Reporting: structural changes are covered by `adopted`/`removeChild`/`detach`. A new
    value needs a `ModelProperty` constant (applied through the public API) and a
    `TrackedProperty` or a setter calling `Changes.valueChanged`. Add the operation to
    `change/ChangeRoundTripTest` and `change/ModelSnapshot`: it fails until it is
    reported and revertible.

## Conventions & deliberate choices

- **JavaFX properties/observable collections in the public API are intentional** (GUI
  binding; `requires transitive javafx.base`). Do not flag or refactor them away.
- Not thread-safe; a dictionary is confined to one thread (FX thread once a UI observes it).
- Javadoc is thorough and explains *why*; match that style (both `/** */` and `///`
  Markdown javadoc are used). Commit messages are descriptive sentences.
- Save is atomic: `LiftWriterSession` writes a temp file and promotes it only on success.
- `RegressionTest` pins bugs found in earlier audits — add a case there for new bug fixes.
- `TODO.md` lists known open issues.
- `src/main/xbj/ChoiceBinding.xjb` is a leftover JAXB binding, not used by the build.
