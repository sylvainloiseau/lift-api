# An observable, encapsulated model with reversible change events: plan (2026-10-13)

## Why

lift-api is used by DictionaryEditor, which needs a complete undo system. The decision
(see DictionaryEditor's `.claude/plan/undo-system-2026-10.md`, section 9) is that lift-api
reports every change of a dictionary as a **reversible event**, and the application builds
its undo history on top of them.

These changes also serve any other consumer:
- **Any UI** can bind to every piece of state, not only to part of it.
- **Batch scripts and importers** can run a block of changes and roll it back if it fails.
- **Tools** can report what a script changed ("dry run", change logs).
- **The library itself is safer:** consumers can no longer change the model behind the
  registry's back.

The principle: **lift-api knows how to change and un-change its model consistently; a
consumer decides what the changes mean** (grouping into user actions, labels, undo
history). Nothing in this plan is specific to DictionaryEditor.

---

## 1. Findings

**F1. Collections are writable from outside.**
- Every collection getter returns the live, writable collection: `getSenses()`,
  `getTraits()`, `getNotes()`, `getFields()`, `getAnnotations()`, `getRelations()`,
  `getPronunciations()`, `getVariants()`, `getEtymologies()`, `getReversals()`,
  `getMedias()`, `FeatureSet.getFeatures()`, `LiftHeader.getFieldsAndTraitsDefinitions()`
  (a live `values()` view).
- The `xxxProperty()` accessors return writable `ListProperty`/`MapProperty`, so even
  `.set(anotherList)` works.
- A consumer can therefore add or remove a component without registration, which breaks
  "the one rule" of `CLAUDE.md`. DictionaryEditor does it once
  (`FieldDefinitionsView`: `header.getFieldsAndTraitsDefinitions().remove(sel)`).
- Inside the library, `AbstractLiftRoot.detach()` itself goes through these public getters.

**F2. Part of the state is not observable.** Plain fields, with plain setters:

| Class | Plain state |
|---|---|
| `AbstractExtensibleWithoutField` | `dateCreated`, `dateModified` |
| `AbstractIdentifiable` | `id`, `guid` |
| `LiftEntry` | `order`, `dateDeleted` |
| `LiftSense` | `order`, `grammaticalInfo` |
| `GrammaticalInfo` | `value`, `traits` (a plain list) |
| `LiftRelation`, `LiftVariant` | `refId` (the string; `refObject` is a property) |
| `LiftRelation` | `order` |
| `LiftMedia`, `LiftIllustration` | `href` / `url` |
| `LiftReversal` | `main` |
| `Feature` | `id`, `guid`, `superordinateFeature`; label/abbrev are MultiTexts (observable) |
| `FeatureSet` | `guid`, `href` |
| `LiftFieldAndTraitDefinition` | everything: kind, targets, data model, writing system, range |
| `MultiText`, `Form`, `LiftTrait` | the annotation lists (plain `ArrayList`) |
| `LiftHeader` | the field definitions (a plain `HashMap`) |
| `AbstractLiftRoot` | `otherXmlAttributes` (a plain `HashMap`; not meant to be edited, see A6) |

**F3. Two kinds of collections, on purpose.**
- **Lists, where the order is significant:**
  - senses, examples, sub-senses;
  - traits, annotations, relations, variants, pronunciations, etymologies, reversals,
    medias, illustrations;
  - the entries of the dictionary.
- **Maps, where it isn't** (like sets, keyed by type, name or language):
  - notes, fields, translations;
  - the forms of a multitext;
  - the features of a range, the field definitions, the languages.
- A component removed from a map is restored by putting it back under its key, wherever
  it lands. `DictionarySnapshot` already compares maps independently of their order.

**F4. Positions in lists can't all be restored.** Among the lists, only
`addSense(int, …)`, `addExample(int, …)` and the entries (`adoptEntryAt`) insert at a
position. The other lists can only append, so undoing the deletion of a list element would
move it to the end and change the meaningful order.

**F5. No change notification at the dictionary level.**
- A consumer can only listen property by property, and only for the observable part.
- Structural changes show up as several low-level collection changes (the parent's list,
  then the registry's lists), not as the operation that caused them.

**F6. Smaller inconsistencies.**
- `LiftHeader.getFieldsAndTraitsDefinitionsFor(target)` (and the other `…For` methods)
  return an `ObservableList` built on a **snapshot**: it never updates.
- `AbstractIdentifiable.setId` sets the field only. The registry's `entriesByLiftId` /
  `sensesByLiftId` and the references (`HasRefId`) aren't updated when an attached entry
  or sense is renamed. *To confirm with a test.*
- The header has no link to its dictionary. Nothing in the header can report a change.
- `TODO.md`:
  - form-level `<annotation>`s are never registered;
  - "Variant has no type": `LiftVariant.setType` exists, but the type is neither read nor
    written.
- `setUUID` is public, with a TODO ("should be protected"). `setOwningDictionary`,
  `setParent` and `setLanguagesManager` are public too: they are internal wiring that a
  consumer can call.

---

## 2. Principles

1. **One way to change each thing.** Every change goes through a method of the exported
   API (`addX`, `deleteX`, `insertX`, a setter, a writable property). Getters return
   read-only views.
2. **Everything a consumer can read, it can observe.** All state is held in JavaFX
   properties or observable collections (consistent with the deliberate use of JavaFX in
   the public API).
3. **Every change is reported once, as the operation it is.**
   - One event per public operation, on the dictionary.
   - Derived state (the registry's indexes, counters, language occurrences, reference
     counts) never produces events: it follows from the reported changes.
4. **Every event can be reverted and reapplied** through the same public operations, so
   the registry stays consistent.
5. **Order is data where it is meaningful.**
   - A list element can be put back at its position.
   - A map element is put back under its key; maps stay unordered.

---

## 3. Work plan

Five workstreams, A to E, then tests (F) and documentation (G). A, B and C are
independent; D needs all three.

### A. Encapsulation: read-only collections

**A1. Collection getters return read-only views.**
- Lists: `FXCollections.unmodifiableObservableList(list)`; maps:
  `FXCollections.unmodifiableObservableMap(map)`.
- They remain observable, so UI binding is unchanged; they can no longer be modified.
- Create each view once, as a field, not at every call.

**A2. Collection `xxxProperty()` accessors.**
- They currently return writable `ListProperty`/`MapProperty`.
- Either remove them, or make them return the same read-only `ObservableList`/`Map` as
  `getX()`.
- A `ReadOnlyListProperty` is *not* enough: its `get()` returns the underlying mutable
  list.
- **Decided:** deprecated for one version (returning the read-only view meanwhile), then
  removed (phase 5).

**A3. Internal access.**
- The model package uses the mutable collections through package-private accessors
  (`sensesMutable()`…), notably in `AbstractLiftRoot.detach()`.
- `DictionaryMutator` keeps calling `addX`/`deleteX` only.

**A4. Header.**
- `getFieldsAndTraitsDefinitions()` returns a read-only view.
- New `addFieldOrTraitDefinition(def)` / `removeFieldOrTraitDefinition(def)`.
  - Removal is refused while a field or trait uses the definition, as for referenced
    entries.
  - The `…For(target)` methods become live filtered views (F6).

**A5. Internal wiring is not public.**
- `setUUID`, `setOwningDictionary`, `setParent`, `setLanguagesManager` become
  package-private, or move behind `internal` (the TODO already asks for it).
- Builders that need them go through `DictionaryMutator`.

**A6. `otherXmlAttributes`.** Not data a consumer edits: the reader keeps there the
attributes it doesn't know, just in case, and the map should normally be empty (the writer
doesn't even write it back today).
- `getOtherXmlAttributes()` returns an unmodifiable plain `Map` (not observable).
- The reader fills it through package-private access; no public setter.
- It has no `ModelProperty` and emits no event; F1 and `DictionarySnapshot` ignore it.

### B. Complete the observable state

**B1. Every field of F2 becomes a JavaFX property**, following the existing patterns:
- **Writable values a UI edits directly** become writable properties:
  - dates, `order`, `href`, `guid`;
  - the annotation's `value`/`who`/`when`, which already are;
  - the trait value;
  - a feature's superordinate;
  - the definition's attributes.
- **Values whose change has consequences** keep a setter that does the work, and expose
  a **read-only** property, like `typeProperty()` today:
  - `id`: updates `entriesByLiftId`/`sensesByLiftId`, and is refused if the id is taken
    (F6);
  - `refId`: updates the reference counts;
  - `Feature.id`: `FeatureSet.changeFeatureId`;
  - `LiftSense.grammaticalInfo`: `setGrammaticalInfo`, `clearGrammaticalInfo`;
  - `LiftReversal.main`.
- **`Optional` getters stay as they are** (`Optional<String> getDateCreated()`). The
  property holds a nullable value; `dateCreatedProperty()` is an
  `ObjectProperty<String>`, or a `StringProperty` where an empty string has no meaning.
- **Properties are created lazily** (see D6): the value lives in a plain field until
  `xxxProperty()` is first called, as JavaFX's own controls do. The getter and setter work
  on the field, or on the property once it exists.

**B2. Annotation lists** (`MultiText`, `Form`, `LiftTrait`) and `GrammaticalInfo.traits`
become observable lists, read-only from outside (A), with `addAnnotation`/
`deleteAnnotation` as their only mutators. They already exist; they only need to manage
the observable list.

**B3. Form-level annotations are registered** (`TODO.md`): `DictionaryMutator.childrenOf`
and the registers include them, so they are reported and found like the others.

**B4. Variant type** (`TODO.md`). **Decided: removed.** A variant's type is the
`variant-type` trait, as FieldWorks writes it, and needs nothing special.
- `LiftVariant`: remove `getType`/`setType`/`assignType`/`typeProperty`, and
  `implements HasType`.
- `LiftEntry.retypeVariant`, `VariantBuilder.withType`,
  `LiftDictionaryBuilder.withVariantType`: removed.
- `LiftHeader`: remove `getVariantTypeManager()` and the built-in `variant-type` feature
  set. A `variant-type` range read from a file is an ordinary range
  (`getFeatureSet("variant-type")`), like any other.
- Tests: `RetypeTest` and `RegressionTest` lose their variant-type cases.
- **DictionaryEditor impact:** `VariantEditor` already edits the `variant-type` trait, but
  takes its choices from `getVariantTypeManager()`; it must read them from the
  `variant-type` range (or the trait definition's option range) instead.

**B5. The header knows its dictionary.**
- `LiftHeader` gets a package-private link to its `LiftDictionary`, set when the
  dictionary creates it.
- `FeatureSet`, `Feature` and `LiftFieldAndTraitDefinition` reach it through their
  owner, so header changes can be reported (D).

### C. Positions in lists

Only the collections whose order is meaningful (lists, F3) need positions. Maps stay as
they are.

**C1. Indexed insertion for every list-held component.**
- `addX(int index, X x)` next to every `addX(X)`, on the parents and in the `Has*`
  interfaces, for:
  - traits, annotations, relations, variants, pronunciations, etymologies, reversals,
    medias, illustrations;
  - and the existing `addSense`/`addExample`.
- `addX(X)` stays as "append".

**C2. Position of a list element.** A single `AbstractLiftRoot.positionInParent()`
(`OptionalInt`: empty for a map-held or parentless component), or one `indexOfX` per
list. That is what a removal event records (D).

### D. Change events

**D1. The change model: a new exported package `fr.cnrs.lacito.liftapi.change`.**

```java
/// A change of a dictionary, already applied when it is reported.
/// Reverting it, then reapplying it, goes through the public API, so the dictionary's
/// indexes stay consistent; both emit their own events.
public sealed interface ModelChange
    permits ValueChanged, ComponentAdded, ComponentRemoved,
            FormAdded, FormRemoved,
            FeatureAdded, FeatureRemoved, FeatureSetAdded, FeatureSetRemoved,
            DefinitionAdded, DefinitionRemoved,
            LanguageAdded, LanguageRemoved {

    LiftDictionary dictionary();
    /// The object the change is about: the component, the form, the feature…
    Object subject();
    void revert();
    void reapply();
}

/// A value replaced: a text, a date, a type, an id, a trait value…
public record ValueChanged<T>(Object owner, ModelProperty<T> property, T oldValue, T newValue)
    implements ModelChange { … }

/// A component attached to a parent (or an entry to the dictionary).
/// `position` is the index in a list; empty for a map-held component (a note, a field…),
/// which a revert puts back under its key.
public record ComponentAdded(Object parent, AbstractLiftRoot component, OptionalInt position)
    implements ModelChange { … }

/// A component removed from its parent; `unregistered` tells deleteX from detach().
public record ComponentRemoved(Object parent, AbstractLiftRoot component, OptionalInt position,
                               boolean unregistered) implements ModelChange { … }

public record FormAdded(MultiText text, Form form)   implements ModelChange { … }
public record FormRemoved(MultiText text, Form form) implements ModelChange { … }
// …and the same shape for features, feature sets, definitions, languages.
```

**`ModelProperty<T>`** is a typed descriptor, one constant per kind of value
(`ModelProperty.DATE_MODIFIED`, `FORM_TEXT`, `TRAIT_VALUE`, `TYPE`, `LIFT_ID`,
`FEATURE_ID`, `HREF`…):
- it has a stable `name()`, so a consumer can switch on it or label it;
- it applies a value through the **public** path (`setType`, `changeFeatureId`, the
  property's `set`), which is what makes `revert()` safe;
- adding a property means adding a constant, and the round-trip test (F1) checks it.

**Why semantic events rather than collection listeners:**
- one event per operation, carrying the information needed to reverse it;
- the parent, the position (for a list element), and whether the subtree was
  unregistered;
- the registry and counters, which are derived, stay silent.

**D2. Listening.**

```java
// LiftDictionary
public Subscription addChangeListener(Consumer<? super ModelChange> listener);
```

`javafx.util.Subscription` comes from `javafx.base`, which is already required
transitively. Rules:
- **Synchronous**, on the calling thread (the dictionary is confined to one thread).
- **After success only.** An operation that throws reports nothing.
- **Listeners must not modify the dictionary** while an event is delivered. A guard
  throws `IllegalStateException`, which keeps the event order meaningful.
- **Only attached objects report.** An object is attached when its dictionary can be
  reached (`owningDictionary()`, or for a `MultiText`/`Form` through its owner, or for the
  header through B5).
  - Building a detached subtree (builders, the reader before adoption) is silent,
    including the `MultiText.add`, `Form.append` and `setX` calls made on it.
  - Attaching the subtree is one `ComponentAdded`, whose revert removes the whole subtree.
    The event holds the object itself, so a reapply puts back the same subtree, with all
    the forms and values it was built with. Nothing is lost by not reporting the
    construction steps.
- **Nothing is emitted while nobody listens.** Every emission point starts with
  `if (!dictionary.isObserved()) return;`, a single field read, before computing an old
  value, a position, or allocating an event (D6).
- **Loading reports nothing**, by construction rather than by a flag:
  - `loadDictionaryFromFile` and `LiftDictionaryBuilder.build()` create the dictionary and
    return it only once complete, so no listener can exist while the file is read;
  - every mutation of the load therefore takes the fast path above: the entries, which
    the reader builds detached and adds with `addEntry`; the header and ranges, which are
    attached from the start; the languages registered during adoption; the
    post-processing (range dereferencing).
  - So the reader never produces a partial stream of events (some forms reported, their
    entries not): it produces none.
  - A future "import into an open dictionary" is not loading: it should build each entry
    detached and `addEntry` it, which reports one `ComponentAdded` per entry and can be
    undone.

**D3. Where events are emitted.** Few places, which is what makes this tractable:

| Change | Emitted from |
|---|---|
| Component added / removed | `AbstractLiftRoot.adopted` / `orphaned` (called by every `addX`/`deleteX` on an attached parent), `detach()`, `DictionaryMutator.attach` (builders), `LiftDictionary.addEntry`/`removeEntry` |
| Writable values | A small family of internal property classes (`TrackedStringProperty`, `TrackedObjectProperty<T>`…) that override `invalidated()`, keep the previous value, and report `(owner, ModelProperty, old, new)`. No listener objects are added per property, and bidirectional bindings are reported like any other `set`. |
| Values with a setter (type, id, refId, grammatical info…) | The setter, after it succeeded |
| Forms | `MultiText.add`/`removeForm`, and `formTextProperty(lang).set("")` (which removes the form: reported as `FormRemoved`, carrying the `Form` object and its annotations, so that a revert brings back the same object) |
| Header | `FeatureSet` (add, remove, `changeFeatureId`), `LiftHeader` (feature sets, definitions), the language managers |

**Nested operations report once.** `DictionaryMutator.attach` → `wire` → `addX` →
`adopted` must produce a single `ComponentAdded`. An internal depth counter makes only
the outermost public operation emit.

**D4. Cascades.** An operation that changes several things reports each change, in order:
- `FeatureSet.changeFeatureId`, if it rewrites trait values;
- adopting a subtree whose forms use a language the dictionary didn't have yet: the
  outermost operation reports `ComponentAdded`, then `LanguageAdded` (otherwise undoing
  the addition would leave the language registered, and F1 would fail). The depth counter
  of D3 must not swallow these: cascade events are emitted explicitly by the outermost
  operation.
- **Removing a language that still has forms: decided, it stays refused**, as now. The
  consumer removes the forms first, each one reported.

Reverting the events in reverse order restores the state. A consumer that wants one
"step" groups them (D5), and the library doesn't need to know about steps.

**D5. `ChangeRecorder`: a transaction primitive.**

```java
try (ChangeRecorder rec = dictionary.recordChanges()) {
    …                      // any changes
    rec.commit();          // keep them; without commit, close() reverts them all
}
rec.changes();             // the changes, in order, e.g. for a report
```

- Useful on its own (importers, scripts, tests), and it's the building block a consumer's
  undo history uses: an undo step is "the changes recorded during one user action".
- Recorders can nest; a nested one reverts only its own changes.
- **Not in lift-api:** labels, merging, save points, history limits. They are consumer
  policy.

**D6. Cost.** The concern is reading files (tens of thousands of objects), not editing:
an edit by hand is one operation, and an event or two per keystroke is negligible.

| Item | Cost while reading | Cost while editing | Measure |
|---|---|---|---|
| Emission (D2, D3) | One field read per mutation (`isObserved()` is false): no old value computed, no position, no allocation | One small record per change; `positionInParent()` is an `indexOf` on a short list, on removal only | Fast path first in every emission point |
| "Attached?" test | Not reached: the fast path comes first | A walk up a few parents | Order of the two tests |
| Nesting depth counter | Not touched (behind the fast path) | An `int` increment | — |
| B1 properties | **Memory** is the real cost: one property object (~40 bytes) per value per object if created eagerly; on a 50,000-entry FieldWorks dictionary (~300,000 objects, ~6 values each) that is tens of MB | — | **Lazy properties** (B1): a plain field until `xxxProperty()` is called. Only what a UI binds is ever created. |
| A1 read-only views | One wrapper per collection per object if eager, and `unmodifiableObservableList` registers a listener on its source | — | Create each view on the first `getX()` call and cache it. |
| E5 sorting when writing | — | When saving: a sort of a few elements per map | Negligible next to the I/O |

So with the fast path and lazy creation, reading costs what it costs today. F5 checks
it.

### E. Consistency fixes found on the way

- **E1. `setId`** on an attached entry or sense updates the LIFT-id indexes, and is
  refused when the id is taken (`DuplicateIdException`). A `RegressionTest` case first
  confirms the current behaviour (F6).
- **E2. Naming:** document the convention in `model/package-info.java`:
  - `deleteX` for components (unlinks and unregisters);
  - `detach()` to move (unlinks, keeps registration);
  - `removeX` for things that aren't components (forms, features, languages,
    definitions).
  No renaming, which would only break consumers.
- **E3. Live `…For(target)` views** in `LiftHeader` (A4).
- **E4. `TODO.md` items** B3 and B4.
- **E5. Stable files: map-held elements are written sorted by key.** Today they are
  written in the maps' iteration order, which for maps keyed by a `Feature` (identity
  hash) can change from one run to the next: saving an unchanged file twice may reorder
  its notes, which shows in text diffs (version control).
  - In the writers only; the model keeps its hash maps.
  - Keys: forms by language; fields by name; notes, translations by the type's id;
    feature sets, features, field definitions by id.
  - Lists keep their order (it is data).
  - `RoundTripTest` gains a case: load, save, load, save; the two files are identical
    byte for byte.

### F. Tests and enforcement

**F1. The round-trip property test (the decisive one).** For a fixed list of seeds
(plain JUnit 6 `@ParameterizedTest`, no new dependency):
1. Load the corpus (`20240828Lift.lift`) and take a snapshot S0 (`DictionarySnapshot`:
   ordered for lists, order-independent for maps, as it already is).
2. Subscribe a listener that records every event.
3. Apply N random operations through the public API. They cover every component kind
   (add at a random position, delete, detach and re-attach elsewhere), every
   `ModelProperty`, forms, types, features, definitions and languages.
4. **After each operation**, compare a snapshot with the previous one: if the dictionary
   changed and no event was emitted, fail, naming the operation. This makes "every
   mutator reports" testable.
5. Take S1. Revert all events in reverse order: the snapshot equals S0, and the registry
   passes the census checks. Reapply all: it equals S1.
6. A failure prints the seed and the operation sequence.

**F2. Encapsulation test.**
- By reflection over the exported model classes: every public getter returning a
  `Collection` or `Map`, called on a populated object, must throw
  `UnsupportedOperationException` on `add`/`put`/`clear`.
- No public method returns a writable `ListProperty`, `MapProperty` or `SetProperty`.

**F3. Event unit tests** for each event kind:
- emitted once;
- not emitted on a detached object;
- loading: a package-private counter of the events created stays at 0 after reading the
  corpus (the fast path really is taken);
- not emitted when the operation throws;
- a listener that mutates gets `IllegalStateException`;
- nested operations emit once;
- `ChangeRecorder` rollback, nesting, and commit.

**F4. Existing tests stay green.** `DictionaryCensusTest`, `RoundTripTest`,
`AttachmentTest`, `DeletionTest` and `RetypeTest` are adapted only where they used the
writable collections (and the variant type, B4).

**F5. Loading cost (D6).** Before phase 2 and after phase 4, measure loading the corpus
(and a large FieldWorks file if one is available): time (median of a few runs after a
warm-up) and retained heap after a GC. Not an asserted test, which would be flaky, but a
small `main` or a test tagged `perf`, excluded by default, whose figures go in the phase
commit message. A clear regression (say more than 10 %) is investigated before release.

### G. Documentation

- `change/package-info.java`: the event model, the rules of D2, and how to build an
  undo history or a transaction on it.
- `model/package-info.java`: the naming convention (E2), read-only views, and positions.
- `CLAUDE.md`, the "Checklist: adding/changing a component kind" gains:
  - **8.** Read-only getter and package-private mutable accessor (A).
  - **9.** For a list-held component, `addX(int, X)` and its position (C).
  - **10.** Reporting (D): structural changes are covered by `adopted`/`orphaned`/
    `detach`; a new value needs a `Tracked*` property and a `ModelProperty` constant.
    `F1` fails until it is done.
- `README.md`: a short "Listening to changes" and "Transactions" section with examples.

---

## 4. Phases

Each phase keeps all the tests passing and can be released on its own.

| Phase | Content | Breaking for consumers? |
|---|---|---|
| 1 | **C1–C2** indexed insertion in lists, positions; **E1** confirmed by a regression test; **E5** sorted writing; **F5** baseline measure | No (additions; saved files may be reordered once) |
| 2 | **B1–B5** observable state (lazy), header link; variant type removed; E1 fixed | **Yes, B4**: the variant type API is gone. In DictionaryEditor: `VariantEditor` reads its choices from the `variant-type` range. |
| 3 | **A1–A6** read-only views, deprecated collection properties | **Yes**: code that modified a returned collection must call `addX`/`deleteX`. In DictionaryEditor: one place (`FieldDefinitionsView`, field definitions). |
| 4 | **D1–D6** change events and `ChangeRecorder`; **F1–F3**, F5 measured again | No (additions) |
| 5 | **G** documentation; removal of the deprecated collection properties | Yes, if a consumer still uses them |

Phases 1 to 3 can be done in any order. Phase 4 depends on all of them: events need
complete state (B), one way to change it (A), and positions to revert (C).

---

## 5. Decisions (2026-10-14)

1. **Variant type (B4):** removed; the type is the `variant-type` trait.
2. **Collection `xxxProperty()` accessors (A2):** deprecated for one version, then
   removed.
3. **Removing a language that still has forms (D4):** stays refused.
4. **`otherXmlAttributes` (A6):** read-only, not observable, not reported.
5. **Map order when writing (E5):** sorted by key, so that files are stable.
6. **Loading cost (D2, D6):** no loading flag; the "nobody listens" fast path, lazy
   properties and lazy views keep reading at today's cost, checked by F5.

---

## 6. Implementation (2026-10-14)

Phases 1 to 4 and the documentation of phase 5 are done; the deprecated methods are kept
for one version, so their removal (the rest of phase 5) is for the next one. 158 tests
(120 before), all passing.

**Where the implementation differs from the plan, and why:**
- **No depth counter (D3).** Emission sits at primitive points (`adopted`, `removeChild`,
  `detach`, the tracked properties, the setters), so a nested operation reports once
  without one. Cascades are separate events, in order: a `LanguageAdded` before the
  `ComponentAdded` that brings the language; the LIFT id the dictionary gives a new sense
  (a `ValueChanged` before its `ComponentAdded`); replacing a grammatical info or a
  reversal's main reports the removal of the previous one.
- **The fast path is global (D2, D6).** `ChangeSupport.anyObserved()` is a counter shared
  by all dictionaries: asking a dictionary whether it is observed would first need the
  walk to find it.
- **`ComponentAdded` has a `registered` flag**: reverting the re-attachment of a detached
  subtree (a move) must detach it, not unregister it. Builders now wire through `addX`
  (instead of registering first), so they report and register like any caller.
- **Lazy lists on `Form` and `MultiText` (D6).** Their annotation lists are allocated on
  demand, with `hasAnnotations()` for internal code: allocated eagerly, they cost +15 %
  of heap.
- **The value of a form's text is its markup** (`Form.getMarkup()`, new): `toString()`
  wraps the root span and does not parse back.
- **`Form.getMultiText()`** (new, public): the owner of a text change is a `Form`; this
  leads to its multitext, then `MultiText.getParent()` to the component.

**Defects found on the way (by the new tests) and fixed:**
- The registry's component lists and `FeatureSet.featuresProperty()` were writable
  (a `ReadOnlyListWrapper` over a writable list: `get()` returns it).
- `Form.changeText` mis-parsed two sibling spans (greedy regex); `href` was dropped. A
  small parser replaces the regex, and spans render their `href`.
- Changing the data model of a definition that fields or traits use broke them: refused
  now (`IllegalStateException`). An absent or unknown data model means STRING, undeclared
  (it used to leave it empty, on which `new LiftTrait` threw).
- Releasing a relation or variant used only its `refObject` (an NPE with a `ref` alone);
  registration used `ref` first. One `referenceTarget` for both.
- Adoption and release were not atomic: a failure part-way left part of the subtree
  registered. They are all-or-nothing now, and `addX` unwires on failure.
- `LiftExample.sourceProperty()` and `getSource()` disagreed once the property was set.
- `LiftTrait.setValue` on a collection trait added to the value instead of replacing it.
- The annotations of a trait were missing from `childrenOf` (never registered).
- The reader must accept a definition declared twice (FieldWorks does it): the last
  declaration wins, with a warning, as it implicitly did before.

**F5 (loading the 1,927-entry corpus, 25 runs, side by side with the previous commit):**
median 20–21 ms before, 21 ms after; retained heap 26.4–26.6 MB before, 26.7 MB after.

**Breaking for consumers** (DictionaryEditor adapted):
- variant type API removed (B4); `VariantEditor` reads the `variant-type` range;
- collections returned are read-only (`FieldDefinitionsView` now calls
  `removeFieldOrTraitDefinition`);
- several return types narrowed or changed: `List` → `ObservableList` (compatible),
  `FeatureSet.getFeatures()` → `ObservableMap`, `xxxProperty()` of collections →
  `ReadOnly…Property` (deprecated), `LiftField.specificationProperty()` read-only;
- `setOrder(int)` → `setOrder(Integer)` (`null` clears; source compatible);
- `setUUID`, `setOwningDictionary`, `setLanguagesManager`, `LiftExample.setParent` are no
  longer public (reached through `internal.ModelInternals`).
