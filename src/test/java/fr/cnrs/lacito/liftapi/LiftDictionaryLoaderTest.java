package fr.cnrs.lacito.liftapi;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.util.logging.Logger;
import org.junit.Test;

import fr.cnrs.lacito.liftapi.model.DuplicateIdException;

public class LiftDictionaryLoaderTest {

    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());

    @Test
    public void loadAndValidateTinyLift () {
        String[] tiny = {"tiny1_entry.xml", "tiny.xml"};
        for (String t : tiny) {
            LiftDictionary lf = Utils.loadDictionaryForTest("lift/" + t);
        }
    }

    @Test
    public void testReadLarge() {
        Utils.loadDictionaryForTest("lift/20240828Lift.lift");
    }

    
    @Test
    public void testDuplicateEntryIdThrowException () {
        String fileTested = "lift/tinyDuplicateEntryId.xml";
        DuplicateIdException thrown = assertThrows(
           DuplicateIdException.class,
           () -> Utils.loadDictionaryForTest(fileTested)
           );

        assertTrue(thrown.getMessage().contains("Duplicate"));
    }



}
