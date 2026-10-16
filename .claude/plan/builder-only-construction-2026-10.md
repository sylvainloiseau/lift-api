# Every component created through the builder API: plan (2026-10-14)

## Why

Components are created by two layers:

- the **builders** (`builder/`), used by applications and tests;
- **`LiftXMLFactory`**, used by the SAX reader for performance.

The two layers decode the same notions twice: types resolved through the header
managers, ids, dates, references, trait values. They have already drifted apart (F4,
F5), and every new component kind or attribute has to be added to both (checklist steps
5 and 6 of `CLAUDE.md`).

The goal: **the builders are the only code that creates and wires a dictionary
component.** The XML reader only translates XML into builder calls.

Performance is not an obstacle. Measured on `20240828Lift.lift` (2026-10-14, see the
analysis that preceded this plan):

- The XML reader and the builders already share **one registration path**: `addX` →
  `adopted` → `DictionaryMutator.adoptSubtree`. In both, nested components are wired
  into a detached parent and registered by the single `adoptSubtree` of the outermost
  attachment.
- Driving the builders from SAX keeps that order: `build()` is called at the end tag, so
  children are built before parents, and the entry's `build()` registers the subtree
  once, exactly as `addEntryToDictionary` does now.
- The only extra cost is one short-lived builder object per component (~14k per load).
  Estimate < 0.5 ms out of 17 ms. Budget for the change: **≤ 5%** on `LoadingCostTest`.

Context: `Form.getMultiText()` is now public (commit e73f1ec). The route form → multitext
→ component is part of the API, so nothing here needs the model's package-private
accessors to find which component a form, or an annotation on a form, belongs to.

---

## 1. Findings

**F1. Registration is already unified; construction is not.** Each pair below decodes the
same thing in two places, sometimes with different idioms
(`hasFeature`/`addFeature`/`getFeature` vs `getOrCreateFeature`):

| XML factory | Builder |
|---|---|
| `createReversal` (inverse type) | `ReversalBuilder.withType` |
| `createEtymology` (etymology type, source) | `EtymologyBuilder` constructor |
| `createRelation` (relation type) | `RelationBuilder` constructor |
| `createNote` (note type) | `NoteBuilder` constructor |
| `createAnnotation` (annotation type, value/who/when) | `AnnotationBuilder` |
| `getTranslationType` | `TranslationBuilder`, `ExampleBuilder.addTranslation` |
| `createField` (`getOrCreateFieldDefinitions`) | `FieldBuilder` |
| `createTrait` (value typed by data model) | `TraitBuilder` (`LiftTrait.setValue(String)`) |
| `setGrammaticalInfo` | `GrammaticalInfoBuilder.resolve` |
| `populateWithAttribute` (id, guid, dates, order, source, ref, dateDeleted) | `withId`, `withGuid`, `dateCreated`... (partial, F3) |

**F2. Builder checks reject valid LIFT files.**
- `SenseBuilder.build()` requires a gloss or a definition.
- `ExampleBuilder.build()` requires an example form.
- `EntryBuilder.build()` requires a lexical-unit form.
- The schema requires none of these. The reference corpus has 6 senses with neither
  gloss nor definition, and 6 examples without a form, so it could not be loaded through
  the builders as they are.

**F3. What the builders cannot express yet.**
- **Attributes:**
  - `dateDeleted` on entries;
  - unknown XML attributes (`otherXmlAttributes`, stored only through
    `ModelInternals.putOtherXmlAttribute`, and they must stay non-public; despite what
    this finding first said, they are not written back, see §7);
  - `order` and `guid` wherever `populateWithAttribute` accepts them but no builder method
    exists (to inventory precisely in step P2.1).
- **Components without a builder:** `LiftMedia` (in a pronunciation), `LiftIllustration`
  (in a sense), the `<main>` of a reversal, `LiftFieldAndTraitDefinition` (header).
- **Wiring:** `DictionaryMutator.wire()` has no case for `LiftMedia` or `LiftIllustration`.
  And because `LiftReversal` implements `HasReversal`, `wire()` would add a `<main>` with
  `addReversal`, as a nested reversal, instead of `setMain`.
- **Forward references:** `RelationBuilder.withRefId` and `VariantBuilder.withRefId` look
  the target up at once. During a load the target is often an entry further down the
  file, so the reference would be lost: `VariantBuilder` even sets a `null` ref object
  and never records the id. This is also a latent bug for any batch import that creates
  entries in arbitrary order.

**F4. Trait values are parsed differently by the two layers.**

| | `LiftXMLFactory.createTrait` | `LiftTrait.setValue(String)` (used by `TraitBuilder`) |
|---|---|---|
| unknown `FEATURE` value | created (`getOrCreateFeature`) | `IllegalArgumentException` |
| `FEATURE_SET`/`FEATURE_LIST` separator | whitespace (`\s+`) | comma |

`LiftTrait.getValue()`, which the writer uses, joins with `", "`. Reading back `"a, b"`
with the factory's whitespace split gives the ids `a,` and `b`. That is very likely a
round-trip bug for multi-valued traits today. The corpus has none, which is why
`RoundTripTest` passes.

**F5. Open world vs closed world.** Loading must accept what the file contains and
declare it: new types, new features, new languages (`registerMultiText` already does
this for languages). The builders already create types on demand
(`getOrCreateFeature`). Only the trait values (F4) and the checks (F2) are stricter than
a file allows.

**F6. The builder style does not fit SAX as is.**
- Builders are configured through nested `Consumer` lambdas (`addSense(s -> ...)`), while
  SAX delivers start and end events.
- What SAX needs instead is "open" builders kept on a stack, with `build()` called at the
  end tag.
- The factory methods already take a *model* parent (`sense(HasSense parent)`). The
  parent can be the element of a builder that is not built yet: wiring into a detached
  parent is exactly what nested builders do today. The handler only needs to reach that
  element, which the builders keep `protected`.

---

## 2. Principles

1. **Builders accept what LIFT accepts.** A builder enforces the schema (required
   attributes such as a relation's type, an etymology's source, a media's href), not
   editorial policy. Editorial policy ("a new sense must have a gloss") belongs to the
   application. See decision D1.
2. **One construction layer, one registration path.** Builders create and wire. `attach`
   → `addX` → `adoptSubtree` registers. Nothing else does either.
3. **The XML layer knows XML, not the model.**
   - It knows element and attribute names, the 0.13/0.15 spellings, the order of events,
     and resolving the header at its end.
   - It does not know how a type is resolved or how a component is wired.
4. **No loading mode.** No flag that makes the model behave differently while a file is
   read. Leniency (F2, F5) is the normal behaviour of the builders. Strictness is
   opt-in, for the applications that want it.
5. **No new public surface for XML needs.** Whatever only the reader needs goes through
   a bridge in `internal`, as `ModelInternals` does: the builder's element, raw XML
   attributes.

---

## 3. Target design

```
SAX events
   │
   ▼
LiftSaxHandler        structure: stack of open builders, open forms, text/spans
   │ element + Attributes
   ▼
LiftXmlDecoder        XML knowledge: names, 0.13/0.15 spellings, unknown attributes,
   │                  header end-resolution, end-of-document reference resolution
   │ builder calls
   ▼
builder/*             the only construction layer: types, defaults, values, wiring
   │ attach() → parent.addX(child) → adopted()
   ▼
DictionaryMutator     the only registration: adoptSubtree / releaseSubtree
```

**Handler mechanics.**
- `Deque<AbstractLiftElementBuilder<?, ?>> builders` replaces `elementStack`.
- **Component start tag:** the decoder creates the child builder on the element of the
  builder on top of the stack, for example
  `factory.sense(BUILDERS.element(top))`, applies the attributes, and pushes it.
- **Component end tag:** pop, then `build()`.
  - For a nested component, this wires it into a detached parent, so nothing is
    registered yet.
  - For an `<entry>`, `build()` → `LiftDictionary.addEntry` → one `adoptSubtree`, as
    today.
- **Forms stay as they are.** A `Form` is a value, not a component. The handler keeps
  adding forms to the open MultiText (`MultiText.add`, the same public call the builders
  make) and appending text and spans to the open form. `multiTextStack` and `formStack`
  stay.
- **Annotations on a text or a form are components** and go through `AnnotationBuilder`,
  with the open `Form` or `MultiText` as parent (both are `HasAnnotation`).
- **`<gloss>`** goes through `SenseBuilder.withGloss(Form)` /
  `EtymologyBuilder.addGloss(Form)`, called with the form at its start tag so that text
  and spans fill it afterwards. Same for every form slot that has a builder method.
- **Header:** `FeatureSetBuilder`, `FeatureBuilder` and a new `FieldDefinitionBuilder`.
  The deferred resolution of `option-range` and `range-element/@parent` stays in the
  decoder, at `</header>`.
- **References:** builders record the referenced id (F3). Resolving ids to objects
  happens at the end of the document, as `dereferenceHasRefTargets` does now, or at once
  when an application builds a reference to an existing target.

**`LiftXMLFactory` disappears.** What remains of it is XML-specific and moves into
`LiftXmlDecoder` (`xml` package, not exported):
- turning `Attributes` into builder calls (today's `populateWithAttribute`, per kind);
- version spellings (`@name` vs `@type` on fields, `<field tag>` in a 0.13 header);
- unknown attributes;
- header end-resolution and end-of-document reference resolution.

It is kept apart from the handler on purpose: the handler answers "which element is
open", the decoder "what this attribute means".

---

## 4. Work plan

### P0. Baseline and pre-existing bugs

1. Record `LoadingCostTest` figures in section 7.
2. **Fix F4 first.** Decide the separator (decision D2). Make `LiftTrait.getValue()` and
   both parsers agree. Add a `RegressionTest` case: a `FEATURE_SET` trait with two
   values survives load → save → load.
3. **Fix the eager `withRefId` (F3)** in `RelationBuilder` and `VariantBuilder`:
   - always set the `refId` string;
   - set the ref object only if the target exists;
   - otherwise leave it to end-of-load resolution, or to `LiftDictionary.addEntry` of the
     target (decision D3).
   - `RegressionTest`: a variant referring to an entry built after it keeps its reference.

### P1. One place per notion

1. Type resolution: every builder uses `getOrCreateFeature` on the right header
   manager. Remove the `hasFeature`/`addFeature`/`getFeature` triples.
2. Trait values: move the data-model switch of `LiftXMLFactory.createTrait` into the model
   as a single parser, e.g. a static `LiftTrait.parseValue(definition, String)` that
   creates unknown features (F5). `TraitBuilder` and the factory both call it.
3. Make the factory delegate its type resolution and trait parsing to the code from P1.1
   and P1.2. No behaviour change, and the duplicates of F1 are gone before the switch.

### P2. Complete the builders

1. **Inventory**, attribute by attribute, what `populateWithAttribute` and the other
   `createX` methods set, against the builder methods. Write the table in this file.
2. **Missing setters:** `EntryBuilder.withDateDeleted`, `order`/`guid` where the inventory
   shows a gap.
3. **Missing builders:**
   - `MediaBuilder` (parent `LiftPronunciation`, `href` required, label);
   - `IllustrationBuilder` (parent `LiftSense`, `href` required, label);
   - `ReversalBuilder.withMain(Consumer<ReversalBuilder>)` plus a main-slot builder;
   - `FieldDefinitionBuilder` (header: name, kind, class/targets, type/data model,
     option-range, writing system, label, description).
   - Factory methods in `DictionaryComponentBuilderFactory` for each.
4. **`DictionaryMutator.wire()`:** cases for `LiftMedia` (`addMedia`) and `LiftIllustration`
   (`addIllustration`). Wiring a reversal must say which slot it goes into: pass the slot
   explicitly, or give the main builder its own `attach` that calls `setMain`. Today the
   type of the child alone decides, which is ambiguous for `LiftReversal`.
5. **Decision D1 applied to `SenseBuilder`, `ExampleBuilder`, `EntryBuilder`.**
6. **Header MultiTexts registered (D4).** In `DictionaryMutator`, a register/release pair
   for the MultiTexts of a feature set, a feature or a field definition, as
   meta-language texts. It is called by `LiftHeader.addFeatureSet`,
   `FeatureSet.addFeature` and `LiftHeader.addFieldOrTraitDefinition` (and their removals)
   once the header belongs to a dictionary. A feature set's features are covered when the
   set is added. The components themselves get no UUID and no index.
   - Order during a load: the header is read before the entries, so its languages are
     declared on load (as `registerMultiText` does for entries), and a form added
     afterwards in an undeclared language is refused.
   - Undo: the header's own `FeatureSetRemoved`/`FeatureAdded`… events stay the only
     events; registration follows the add/remove they replay.
   - Tests: a feature label in an undeclared language is refused once the dictionary is
     loaded; header languages appear in `LiftDictionaryLanguagesManager` counts;
     removing a feature set releases its texts; `ChangeRoundTripTest` still passes.
7. **Tests:** `builder/` tests for each new builder and setter. `AttachmentTest` cases for
   media, illustration and main (registered when built into an attached parent; not
   registered, then registered once, when built into a detached one).

### P3. The bridge

`internal/BuilderInternals`, on the `ModelInternals` pattern: an `Access` interface
implemented in package `builder` and installed by a static initializer. It exposes:

- `element(builder)`: the element under construction, for the handler to create child
  builders on it;
- `putOtherXmlAttribute(builder, name, value)`: delegates to
  `ModelInternals.putOtherXmlAttribute`;
- whatever else P2.1 shows the reader needs and applications must not see.

`EncapsulationTest`: add a check that no public builder method exposes the element
before `build()`.

### P4. Switch the reader

1. `LiftXmlDecoder` replaces `LiftXMLFactory`: per element, the builder to create and the
   attribute mapping.
2. `LiftSaxHandler`: builder stack (section 3). The two `switch`es keep their structure:
   the first creates and pushes builders, the second pushes MultiTexts. The end-tag
   switch calls `build()`.
3. Delete `LiftXMLFactory`.
4. Error reporting: an exception from a builder during a load must still carry the line
   and column. Wrap it in the handler with the SAX `Locator`
   (`setDocumentLocator`), which the reader does not use yet.
5. **Tests:** the whole suite, in particular:
   - `DictionaryCensusTest` (nothing dropped);
   - `xml/RoundTripTest` (nothing changed);
   - `LiftDictionaryLoaderTest`, plus the strict-mode and unknown-element tests.
6. **Measure:** `LoadingCostTest` before and after, within the ≤ 5% budget. If it is
   exceeded, profile before optimising. The suspects are `childrenOf` (unchanged) and
   builder allocation.

### P5. Documentation

- `CLAUDE.md`:
  - package map (`xml`: decoder instead of factory);
  - "Core design" (one construction layer; drop "the XML reader instead assembles…" as a
    separate route);
  - checklist: merge steps 5 and 6 ("builder + its factory method; decoder mapping +
    writer + vocabulary").
- `DictionaryMutator` class javadoc:
  - the terminology list (line ~80) says attaching is "non recursive… registering each
    node as it goes", which is only true when the parent is already attached;
  - restate the one rule: a subtree is registered the first time it is wired into an
    attached parent;
  - the `adoptSubtree` javadoc's paragraph about the reader.
- `xml/package-info.java`, `builder` package doc (builders accept what LIFT accepts;
  editorial checks are the application's).
- `TODO.md`: remove what this closes.

---

## 5. Decisions to take

**Decided (2026-10-15):**
- **D1: (a).** The editorial checks are removed from `SenseBuilder`, `ExampleBuilder` and
  `EntryBuilder`; builders follow the schema only.
- **D2: commas** (revised 2026-10-15; first decided as "write whitespace, read both").
  `LiftTrait.getValue()` joins with `", "` and the shared parser (P1.2) splits on commas,
  trimming each id. Whitespace cannot separate values: feature ids contain spaces (FLEx
  semantic domains, `"1.5 Plant"`). See §7.
- **D3: option 1.** References to a later target are resolved at the end of a load only;
  batch imports call a public `LiftDictionary.resolveReferences()`.
- **D4, builders: yes.** `FeatureSetBuilder`, `FeatureBuilder` and a new
  `FieldDefinitionBuilder` are the only way the decoder creates header components, for a
  uniform creation interface. **D4, indexes: MultiTexts only** (the middle way below).
  The header components themselves are not indexed and get no UUID; their MultiTexts
  (labels, abbreviations, descriptions) are registered as meta-language texts when the
  component is added to the header, so their languages are declared, counted and
  checked like any other meta-language text.


**D1. Where do the editorial checks of F2 go?**
- **(a) Recommended:** remove them from the builders. Builders follow the schema; an
  application that wants "a sense needs a gloss" checks it itself.
  - DictionaryEditor's call sites already supply the content (`EntryView`:
    `withForm`; `LiftEntryEditor`, `EditorPresenters`, `QuickEntryView`: `withGloss` /
    `withExample`), so they do not rely on the exception.
  - Builder tests that assert these exceptions are updated.
- **(b)** keep them in `build()` and add an unchecked build reachable only through
  `BuilderInternals`. No public API change, but two behaviours per builder: the kind of
  mode principle 4 avoids.

**D2. Separator of multi-valued traits (F4).** Check the LIFT specification and FLEx
exports. Whitespace is what the reader expects. `getValue()` writes `", "`. Writing
whitespace and reading both (split on `[,\s]+`) keeps existing files loadable.

**D3. When does a reference to a later target get its object?**
- **Option 1:** at the end of a load only. Batch imports then call a public
  `LiftDictionary.resolveReferences()`.
- **Option 2:** also whenever an entry or sense is registered, by looking up pending
  references to its id in `DictionaryRegisters`. More general; the cost is one map
  lookup per registered entry or sense.

**D4. Header components.** Feature sets, features and field definitions are not
registered in the indexes. Including them (P2.3 `FieldDefinitionBuilder`, header
handling in P4) completes "every creation through a builder". Leaving the header in
the decoder would halve P2.3 but keep a second construction layer for it.

*Indexing the header components (decided: MultiTexts only, see above).* Today they are reachable only through
`LiftHeader` (`getFeatureSets()`, `getFieldsAndTraitsDefinitions()`) and report their
own `FeatureSetAdded`/`FeatureAdded`/`DefinitionAdded` events; `DictionaryMutator` never
sees them, so they get no UUID and their MultiTexts (labels, abbreviations,
descriptions) are not registered.
- Benefits: (1) their MultiTexts would go through `registerMultiText` like every other
  meta-language text: languages declared and counted in `LiftDictionaryLanguagesManager`,
  forms in an undeclared language refused; (2) one creation-and-registration path for
  the whole document (principle 2), and `childrenOf` would cover the header, so the census
  test could check it; (3) a UUID for each, if a consumer needs a stable key (none does
  today: `Feature` is already compared by identity).
- Costs: (1) a second root besides entries — "attached" is defined as "reaches an entry
  with an owning dictionary", so it must be extended to the header, and `adoptSubtree`,
  `releaseSubtree`, `childrenOf`, `nodesById` and the census get header cases; (2) features
  are mostly created on demand (`getOrCreateFeature`) while entries are being loaded or
  edited: each would now be registered, and removal would have to unregister it and its
  MultiTexts; (3) the header keeps its own events, so removing a feature set would raise
  both its own event and a release, or the events must be unified; (4) about 3 more
  indexes and a few hundred more registrations per load (negligible next to 25,000).
- Middle way: do not index the components, but register only their MultiTexts as
  meta-language texts when they are added to the header. This gives benefit (1), the
  only one with a visible effect, at a fraction of the cost.

---

## 6. Risks

- **Error messages of malformed files change.** Builder exceptions replace factory
  exceptions. P4.4 keeps the position information. Tests matching messages need
  updating.
- **Public API behaviour change** if D1(a): a sense without gloss can now be built.
- **Hidden factory behaviour** missed by the inventory: the census and round-trip tests
  on the corpus and on the small test files are the safety net. Run them after each
  phase, not only at the end.
- **Interaction with the UUID plan** (`fast-internal-uuids-2026-10.md`): independent.
  Doing it first gives this plan a stable performance baseline.

## 7. Results (2026-10-15)

Implemented, uncommitted. `LiftXMLFactory` is deleted; `LiftXmlDecoder` turns elements
into builder calls and `LiftSaxHandler` keeps a stack of open builders (`Frame`: the
component, and its builder or `null` for a component that already existed and is only
filled: the header, a built-in range declared again, a range element declared twice).

### Attribute inventory (P2.1)

What the reader sets, element by element, and the builder call it now goes through.
"kept" means stored as an unknown attribute (`AbstractLiftRoot.getOtherXmlAttributes()`),
logged as a warning and **not** written back (the writer never read them). Until
2026-10-16 the elements of the second group (translation to field-definition) ignored
their other attributes silently ("dropped"); they now hand them to `LiftXmlDecoder.rest`,
which keeps and logs them too.

| Element | Attribute → builder call | Other attributes |
|---|---|---|
| entry | `id` → `withId`, `guid` → `withGuid`, `order` → `withOrder` (since 2026-10-16, see below), `dateDeleted` → `dateDeleted`, `dateCreated`/`dateModified` → `dateCreated`/`dateModified` | kept |
| sense, subsense | `id`, `guid`, `order` → `withOrder`, dates | kept |
| etymology | `type`, `source` → constructor, dates | kept |
| relation | `type` → constructor, `ref` → `withRefId`, `order` → `withOrder`, dates | kept |
| variant | `ref` → `withRefId`, dates | kept |
| example | `source` → `withSource`, dates | kept |
| note | `type` → constructor, dates | kept |
| pronunciation | dates | kept |
| translation | `type` → constructor (default type when absent) | kept (was: dropped) |
| field | `name` (0.15) or `type` (0.13) → constructor, `dateCreated`/`dateModified` → `dateCreated`/`dateModified` (since 2026-10-16) | kept (was: dropped) |
| trait | `name`, `value` → constructor (`LiftTrait.parseValue`) | kept (was: dropped) |
| annotation | `name` → constructor, `value`/`who`/`when` → `withValue`/`withWho`/`withWhen` | kept (was: dropped) |
| grammatical-info | `value` → constructor | kept (was: dropped) |
| reversal | `type` → `withType` | kept (was: dropped) |
| main | — (`reversalMain`) | kept (was: dropped) |
| media, illustration | `href` → constructor (required) | kept (was: dropped) |
| range | `id` → constructor, `href` → `withHref`, `guid` → `withGuid` (new ranges only), dates → `dateCreated`/`dateModified` (since 2026-10-16; on a range the header already has, set on the component) | kept (was: dropped) |
| range-element | `id` → constructor, `guid` → `withGuid`, `parent` → resolved at `</header>`, dates → `dateCreated`/`dateModified` (since 2026-10-16; merged into the first declaration of an element declared twice, like `guid`) | kept (was: dropped) |
| field-definition, 0.13 `<field tag>` | `name`/`tag`/`guid` → constructor, `class` → `withTargets`, `type` → `withDataModel`, `writing-system` → `withWritingSystem`, `option-range` → resolved at `</header>` | kept (was: dropped) |

Keeping the unknown attributes does not change what is written, since they are not
written back.

**Entry `order` (fixed 2026-10-16).** Neither the old factory nor the first version of the
decoder mapped `<entry order>`: it went to the unknown attributes, which are not written,
so a save dropped it (458 entries of the reference corpus have one). The round-trip test
did not see it, since it compares two loads that both lacked it. The decoder now calls
`EntryBuilder.withOrder`; `RegressionTest.theOrderOfAnEntrySurvivesASaveAndAReload` pins
it. The same change logs every unknown attribute: the corpus produces none.

**Every element reports its unknown attributes (2026-10-16).** `LiftXmlDecoder.rest(frame,
attributes, known...)` hands the attributes an element read by name did not ask for to
`other()`. Doing so showed that the dates of field, range and range-element, which the
schema allows and the writer writes, were never read. They now go through
`dateCreated`/`dateModified`, added to `FeatureSetBuilder` and `FeatureBuilder`, and
overridden in `FieldBuilder` to return `FieldBuilder` (the inherited ones returned the base
type, which broke chaining). A range or range element that already exists has no builder,
so its dates are set on the component, as its `guid` already was. The corpus has none, so no file
of ours lost any. `other()` now stores through `ModelInternals` (it also serves the
components that already existed, which have no builder), and
`BuilderInternals.putOtherXmlAttribute` was removed. Tests:
`RegressionTest.anUnknownAttributeOnAnElementReadByNameIsKept`,
`theDatesOfAFieldAndARangeElementSurviveASaveAndAReload`.

### What changed for applications

- D1: `EntryBuilder`, `SenseBuilder`, `ExampleBuilder` no longer refuse an entry
  without lexical unit, a sense without gloss or definition, an example without text.
- Builders accept an empty annotation name, reversal type and part of speech (files
  have them); they refuse `null`. `EtymologyBuilder` now requires a source, and
  `MediaBuilder`/`IllustrationBuilder` an `href` (schema requirements; the old reader
  did not check the media's).
- `TraitBuilder` parses with `LiftTrait.parseValue`, which adds an unknown feature to the
  feature set (as the reader always did); `LiftTrait.setValue(String)` keeps refusing it.
- `RelationBuilder.withRefId` / `VariantBuilder.withRefId` record the id even when the
  target does not exist yet (D3); new public `LiftDictionary.resolveReferences()`.
- New builders: `MediaBuilder`, `IllustrationBuilder`, `FieldDefinitionBuilder`,
  `ReversalBuilder.withMain` / `factory.reversalMain`, plus `factory.feature`,
  `factory.fieldDefinition`, `FeatureBuilder.withSuperordinate`, `ReversalBuilder.withForm`.
- D4: header texts registered as meta-language texts. On the corpus: same language set
  (en, tpi), 301 more registered meta-language texts, 7 more `en` occurrences.
- Errors raised while building during a load carry the line and column of the element
  (`SAXParseException` wrapped in `LiftDocumentLoadingException`; a
  `DuplicateIdException` keeps its type and gains the position in its message).

### Deviation from the work plan

- P1.3 (make the factory delegate before switching) was skipped: the factory was
  deleted in the same change, and the census and round-trip tests ran on the result.

### D2 revised: commas

Implementing D2 as first decided (write whitespace, read both) broke
`RegressionTest.traitCanHoldAFeatureSetAndAFeatureList`: feature ids can contain spaces
(FLEx semantic domains are `"1.5 Plant"`; the corpus has 855 such trait values, all
single-valued), so a multi-valued (`option-collection`/`option-sequence`) trait with such
ids could not round trip (`"1.1 Sky"` read back as `1.1` and `Sky`). Decided instead:
write `", "` (what `getValue()` already wrote) and split on commas only, trimming each
id. The F4 bug was the reader's whitespace split; it is gone. Only an id containing a
comma cannot be part of a multi-valued trait.

### Figures

`20240828Lift.lift` (1,927 entries), JDK 26. Long runs: 300 warm-up loads, then
3 × 200 timed loads, HEAD and the new code alternated twice.

| | HEAD (844ed7b) | Builder-only |
|---|---|---|
| Long runs, median | 12.6 / 13.0 / 12.9 / 13.3 / 13.0 / 13.1 ms (mean 12.98) | 13.3 / 13.1 / 12.7 / 13.8 / 13.7 / 13.3 ms (mean 13.32) |
| `LoadingCostTest`, median | 18 ms | 17 ms |
| Retained heap (`LoadingCostTest`) | 25.9 MB | 25.7 MB |

About +2.6% load time, within the 5% budget.

Tests: 188 (164 before). New: `HeaderTextsTest` (6), `builder/SchemaLevelBuildersTest`
(9), `RegressionTest` (+6: multi-valued trait round trip with an id containing a
space, spaces around ids trimmed, forward references, reference resolved at once, error
position, duplicate id position),
`AttachmentTest` (+2: media, illustration and main, attached and detached),
`EncapsulationTest` (+1: no builder method hands out its element). All pass.
