
## 

LiftDictionaryCounterManager.discoverFields / attachTargetOnField: a TreeSet of non-Comparable definitions (dead code today, §7).
LiftFieldAndTraitDefinition.setResolvedRange has no guard. Changing the range of a definition in use leaves its traits holding features of the old set. Refuse it while used, as setDataModel does. A TODO.md entry.
Built-in ranges declared with an href lose it on save and are written inline (first plan). A TODO.md entry.

- an option trait with no value is written as value="" and read back as a feature with an empty id;
- built-in ranges lose their href on save;
- question 5: an option definition with no option-range taking the range of its own name;
  Q5. An option definition with no option-range. Today its traits fail to parse, and the load fails. Should it take the range of its own name when there is one (LIFT: “resolved by its use in a field-definition”)? The writer would then add the option-range attribute. Suggested as a separate small step, not required by this plan.
- a "new value…" action in the range picker. For the 9 range-named corpus traits, new values can no longer be typed in DictionaryEditor.


# Done

## Correct Trait declaration

I would like you to make a plan for the following change.

The current behavior is that when a TraitBuilder is created, it associates the Trait under construction to a LiftFieldAndTraitDefinition whose id matches the trait's `type`. This is done in TraitBuilder.definition(...), where getOrCreateTraitsDefinitions(...) is called, which means that either a LiftFieldAndTraitDefinition is found, or new one is created on the fly.

However, the logic should be slightly different. Here are the three steps to follow in order to resolve the `type`:

- case 1: Before looking for a LiftFieldAndTraitDefinition matching `type`, the TraitBuilder should first checked if the String `type` matches a FeatureSet ID (called range in Lift).
- case 2: If no matching FeatureSet is found, a LiftAndFieldDefinition matching `type` should be search (but not created).
- case 3: If no LiftAndFieldDefinition is found, the name is a range that is not declared in the header and simply used on the fly.
 
This means that three different kinds of "type" can be recognized on the Trait. This three type should be distinguished because, when writing the dictionary back, a different action should be taken depending of this kind: if the `type` was a direct reference to a range, no field-definition should be written in the header with that name.

Since case 2 and 3 could very well be handled by the already existing LiftFieldAndTraitDefinition, one solution could be to use LiftFieldAndTraitDefinition, but to add a field to it in order to adapt the behavior when needed (for the writer for instance).

One solution therefore could be:

- to add to LiftFieldAndTraitDefinition a field `implicitness` whose type is an Enum DefinitionImplicitness with three values : EXPLICIT_DEFINITION, IMPLICIT_DEFINITION_WITH_DIRECT_RANGE_REFERENCE or IMPLICIT_DEFINITION_WITH_STRING, corresponding to the three kind. When a builder built a trait, if the trait `type` matches a range (case 1), then:
- it creates a LiftFieldAndTraitDefinition,
- set its kind to `DIRECT_RANGE_REFERENCE`,
- set its setResolvedRange(...) to the corresponding range,
- set the setKind(...) to `TRAIT` and
- setDefinitionType to LiftFieldAndTraitDefinitionDataModel.FEATURE 

If the trait's `type` matches an existing LiftAndFieldDefinition (case 2), the trait is wired to it.

If the trait's `type` matches nothing (case 3), then:
- it create a LiftFieldAndTraitDefinition,
- set its kind to `IMPLICIT_DEFINITION_WITH_STRING`,
- set the setKind(...) to `TRAIT` and
- setDefinitionType to LiftFieldAndTraitDefinitionDataModel.STRING 

Moreover:

- In the writer, when writing LiftFieldAndTraitDefinition in the header, the instance with a `implicitness` is different than EXPLICIT_DEFINITION should be skipped.

- This will fix the bug that, when LiftFieldAndTraitDefinition was created on the fly with a `type` that is indeed a range name (case 1), the created LiftFieldAndTraitDefinition has no FEATURE data model and were not pointing at the correct feature set; therefore the range value (feature) pointed at by the corresponding traits where not counted in `LiftHeader.uses(Feature f, FeatureSet ignoring)`.

Please investigate this fix and report issues and bug it may create.

## Varia

- setResolvedRange in a LiftFieldAndTraitDefinition is now refused while the definition is in use;



"setResolvedRange isn't refused while the definition is in use;" Could you explain?

It's about the range of a definition that is already used by traits.

Today. LiftFieldAndTraitDefinition.setResolvedRange(...) sets a definition's option-range, meaning the feature set its trait values come from. Nothing stops you calling it while traits use the definition. The traits keep the Feature objects they hold, which belong to the old range.

## external ranges files plan

Please make a plan for fixing the href issue : href attribute on range element in the header. Note that next to test file "20240828Lift.lift" there is now a 20240828Lift.lift-ranges file, towards which the test file is making reference in href attributes.

When encountering an href attribute on range element in the header, the sax parser should open the referred file, parse it through a dedicated SAX handler and decoder (the file contain only a lift-ranges root element, containing range element with id attribute, containing range-element element with id attribute, containing label and description element containing multitext). You will create this parser and decoder; the main logic is only a simple version of the existing SAX handler and decoder.

The content of the parsed file should be kept up to the end of the parsing of the header of the main file, so that if other reference to this files is found in the header it is not parsed again.

When a range in the main file has a href element, its id attribute must match an id attribute of a range element in the file refered. If no matching id is found, an error is raised.

For instance if "20240828Lift.lift" contains:

<range id="dialect" href="20240828Lift.lift-ranges"/>

Then "20240828Lift.lift-ranges" must contain a range with id "dialect".

The ranges loaded through reference to an external files cannot be written (re-typed, or have new feature, or have change to any of its feature). The blocking of modification can be tested against the presence of an href property; or (it is your implementation decision) a new boolean "hasExternalReadOnlyDefinition" property, linked to the presence of an href, could be created on featureSet (= range) object, and read by its feature (= range element) before accepting being modified. If this property is turned true, then the href is removed, and then the featureset and its features are writable and will be savec in the header.

In the writter, when featureSet has href propery (or a boolean property linked to it, depending on the implementation decision), it is not written: only a <range> element with an id and an href. The refered file need not be written again, since it has not changed.

In DictionaryEditor, a featureSet with an href cannot be changed (its ID, label, description, feature). A button offer to turn it to an internally-defined feature set in order to make it modifiable.

the href of the test file currently contain relative path "DictionaryLift20261022.lift-ranges". Please note that the href can also contains URI (file:///home/user/DictionaryLift20261022.lift-ranges") and that both syntax should be tried.

Please make this plan, report bugs or issues and suggest useful improvements.

##

If I understand correctly, a feature that is not declared in its featureset (range) in the header is still created silently if it is encountered on a trait in the corpus. It is then written in the header when the file is saved, resulting in a violation of the contract that saving a file just loaded produce the same document as the document read. Is this correct?

lift-api should also flag *Undeclared features of internal ranges*, as are undeclared features comming from an external file, maybe then generalising `isUndeclaredInTheExternalFile()` to "undeclared in its range", set by the reader for any
   set, so that the validator reports them like the external ones. They should not be modifiable. A button in the GUI should offer to turn them into a declared feature, so that they can be modified and saved. A warning should be issued for those too.

You also mention Range not declared at all : your correct, those should be flagged, not modifiable, and not writted down; they should issue a warning (when first encountered, not at every occurrence) and a button in the GUI should offer to turn them into a declared range so that it can be edited and saved.

In GUI, the table of range and range value, undeclared feature should appears in grey, so that the user can spot them easily.

Regarding your paragraph "A second violation turned up:": those built-in ranges are written even when they are not used because they are hard-coded and created by default by the header. I agree with your fix: Record whether the file declared each built-in range. One that wasn't declared would not be written, unless something was explicitly added to it.

Please write the plan according to this.
