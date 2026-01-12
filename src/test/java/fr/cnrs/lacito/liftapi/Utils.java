package fr.cnrs.lacito.liftapi;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

public class Utils {

    protected final static LiftDictionary loadDictionaryForTest(String file) {
        URL resourceUrl = Utils.class.getClassLoader().getResource(file);
        if (resourceUrl == null) throw new IllegalStateException("Ressource is null. Check the url of the test document.");

        // Convert URL to File to get the absolute path
        File resourceFile = new File(resourceUrl.getPath());

        LiftDictionary lf = null;
        try {
            lf = LiftDictionary.loadDictionaryWithFile(resourceFile);
        } catch (LiftDocumentLoadingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return lf;
    }
 
}
