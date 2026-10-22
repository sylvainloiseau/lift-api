package fr.cnrs.lacito.liftapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.Utils;

public class LiftHeaderTest {

    @Test
    public void testGetFieldsAndTraitsDefinitionsFor() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/header_fieldDefinition.xml");
        //System.out.println("--- ");
        //for (LiftFieldAndTraitDefinition f : lf.getHeader().getFieldsAndTraitsDefinitions()) {
        //    System.out.println(f);
        //}
        //System.out.println("--- ");
        assertEquals(3, lf.getHeader().getFieldsAndTraitsDefinitions().size());
        List<LiftFieldAndTraitDefinition> fields = lf.getHeader().getFieldsAndTraitsDefinitionsFor(LiftFieldAndTraitDefinitionTarget.EXAMPLE);
        assertEquals(2, fields.size());
    }

    @Test
    public void testDiscoveredTrait() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/header_fieldDefinitionWithDiscoveredField.xml");
        // The undeclared "morph-type" trait adds no definition to the header: it would be
        // written there on save, declaring what the file never declared.
        assertEquals(3, lf.getHeader().getFieldsAndTraitsDefinitions().size());
        LiftTrait morphType = lf.getLiftDictionaryRegistry().getTraits().get(0);
        assertEquals(new UndeclaredName("morph-type"), morphType.getSpecification());
        assertEquals("stem", morphType.getValue());
    }

}
