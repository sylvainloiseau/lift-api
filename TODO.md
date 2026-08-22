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
