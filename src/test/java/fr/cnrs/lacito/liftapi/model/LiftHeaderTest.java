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
        assertEquals(4, lf.getHeader().getFieldsAndTraitsDefinitions().size());
    }

}
