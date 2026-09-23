Two things remain open, both recorded in the code rather than just in this conversation:

- Form-level annotations — an <annotation> inside a <form>/<gloss> is held by the Form, which isn’t an AbstractLiftRoot, so nothing can reach it. Never registered; its MultiText never counts toward the dictionary’s languages. Documented on DictionaryCensusTest, whose SAX counter excludes precisely those annotations so the rest of the assertion stays strict.
- (done) The node→MultiTexts switch was duplicated between register() and unregister(); it is now stated once, as `DictionaryMutator.objectTextsOf` / `metaTextsOf`, the way `childrenOf` was. Two gaps it made visible are still open: `LiftEtymology.getGlosses()` and `LiftEntry.getCitations()` are MultiTexts that no registration path has ever touched, so their languages never count.


The two creation channels (builder, and the "low-level" one used by the SaxHandler) are
no longer a way to get an inconsistent dictionary, and it cost nothing at parse time.
Rather than forcing every component through a builder, the invariant moved to attach
time: `addX()` registers its argument if, and only if, the receiver is attached to a
dictionary (`AbstractLiftRoot.getOwningDictionary()` / `adopted()`). During a parse an
entry is detached until `</entry>`, so no `addX()` registers anything and the single
`adoptSubtree` traversal still does all the work — no builder is allocated. Loading
20240828Lift.lift is unchanged (min 19 / median 21-23 ms before, min 16-17 / median
22-23 ms after, 15 runs each).

Still worth revisiting: whether `addX()` should become package-private by moving the
mutation core into `model`. The attach-time invariant already makes an unregistered
component sitting in the tree impossible; visibility lockdown would additionally stop
someone from building a *detached* component on purpose, and that is something we want
to keep supporting (cut/paste, undo, moving a sense between entries, and the SAX
parser's own build-then-adopt pattern all depend on it).

more clear: wire for parent<->child, register for uuid in the dictionary


- in the builder: element should not be in super constructor since it prevents checking argument or computing value before creating the element
- mode : strict vs discoverable
    - on field and trait creation; cf. code on both class on setParent :
        on Field:
        // TODO : in Factory or builder rather? depending on the mode (strict vs discoverable) ?
        LiftFieldAndTraitDefinitionTarget target = LiftFieldAndTraitDefinitionTarget.fromType((AbstractLiftRoot) parent);
        if (!nameProperty.get().getTargets().contains(target)) throw new IllegalArgumentException("Parent is not of the expected type");

        on Trait:
        // // TODO : in Factory or builder rather? depending on the mode (strict vs discoverable) ?
        // LiftFieldAndTraitDefinitionTarget target = LiftFieldAndTraitDefinitionTarget.fromType((AbstractLiftRoot) parent);
        // if (!definitionProperty.get().getTargets().contains(target)) throw new IllegalArgumentException("Parent is not of the expected type");
    
- duplicate between PostUnmarshalling and LiftDictionaryFeatureManager... ?
- when removing an object with unregister(), it is not nessesary to call detach() on all descendants.
- in AbstractNotable, the map String->Note can loose sync with the note type id. Should be a map LiftHeaderRangeElement->Note. Idem from translation.
  However those map will always be a problem, with key possibly changing
- remplacer String name par LiftFieldAndTraitDefinition definition dans LiftField et LiftTrait
        //LiftFieldAndTraitDefinition def = header.getFieldsAndTraitsDefinitions(type);
        LiftField f = new LiftField(type);
- last method in PostUnmarshalling
Change to be made to LIFT: 

- tout les new Form notamment dans le package builder crée des entrées non enregisrées dans les multitextes
- dans LiftExemple, le MultiText n'est pas enregistré :         return translationsProperty.computeIfAbsent(type, t -> new MultiText());


- group relation (complex type : derived)
- list lang, type (notes, translation, see HasNote ...)
- mail by SIL developer

mainController
- remplacer les fonctions 
            List<String> fieldTypes = getKnownFieldTypesFor(
et 
            Map<String, Set<String>> traitValues = getKnownTraitValues();
            List<String> annotationNames = getKnownAnnotationNames();
etc. par :
            List<String> fieldTypes = currentDictionary.getHeader().getFieldsAndTraitsDefinitionsFor (
etc.

- remplacer FieldEditor(String) par (LiftFieldAndTraitDefinition)
