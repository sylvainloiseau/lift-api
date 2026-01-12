package fr.cnrs.lacito.liftapi;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class LiftEntryTest {
    @Test
    public void testGetCreationDate() {
        LiftDictionary lf = Utils.loadDictionaryForTest("lift/tiny.xml");
        Optional<String> dateCreated = lf.liftDictionaryComponents.getAllEntries().get(0).getDateCreated();
        assertEquals("2015-06-19T08:46:53Z", dateCreated.get()); 
    }

}
