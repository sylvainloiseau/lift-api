- The ojbect created in the the LiftXMLFactoryNew are not registered (no call to register() in LiftDictionaryRegistry)
- duplicate between PostUnmarshalling and LiftDictionaryFeatureManager... ?
- when removing an object with unregister(), it is not nessesary to call detach() on all descendants.
- /** duplicate of LiftHeaderRange */
    private class LiftHeaderTypeManager {
- Translation type are created in liftXMLFactoryNew but not stored in LiftExample objects. See LiftSaxHandler l. 481
- in AbstractNotable, the map String->Note can loose sync with the note type id. Should be a map LiftHeaderRangeElement->Note. Idem from translation.
  However those map will always be a problem, with key possibly changing
- appeler transiant les idRef de HasIdRef et les optionRange de LiftFieldAndTraitDefinition
- remplacer String name par LiftFieldAndTraitDefinition definition dans LiftField et LiftTrait
        //LiftFieldAndTraitDefinition def = header.getFieldsAndTraitsDefinitions(type);
        LiftField f = new LiftField(type);
- last method in PostUnmarshalling
Change to be made to LIFT: 

- group relation (complex type : derived)
- list lang, type (notes, translation, see HasNote ...)
- mail by SIL developer
