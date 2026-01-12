package fr.cnrs.lacito.liftapi;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;
import org.junit.Test;

import fr.cnrs.lacito.liftapi.model.DuplicateIdException;

public class LiftDictionaryWriterTest {

    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());

    @Test
    public void loadAndWriteTinyLift () {
        String uri = "lift/20240828Lift.lift";
        LiftDictionary lf = Utils.loadDictionaryForTest(uri);
        try {
            URL resourceUrl = Utils.class.getClassLoader().getResource(uri);
            LOGGER.info("Writing to " + resourceUrl.getPath() + ".written.lift");
            File resourceFile = new File(resourceUrl.getPath() + ".written.lift");
            lf.save(resourceFile);
        } catch (WrittingLiftDocumentException e) {
            e.printStackTrace();
        }
    }

}
