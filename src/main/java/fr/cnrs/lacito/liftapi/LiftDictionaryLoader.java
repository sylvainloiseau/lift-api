package fr.cnrs.lacito.liftapi;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import fr.cnrs.lacito.liftapi.model.LiftFactory;
import fr.cnrs.lacito.liftapi.xml.LiftSaxHandler;

public final class LiftDictionaryLoader {

    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());

    public final static LiftDictionary LoadWithSax(File f, boolean validate) throws LiftDocumentLoadingException {
        // URL schemaUrl = LiftDictionaryLoader.class.getResource("schema/lift-0.13.xsd");
        // File schemaFile = new File(schemaUrl.getPath());
        // if (!schemaFile.exists()) throw new LiftDocumentLoadingException("Schema not found: " + schemaFile.getAbsoluteFile());
        // LOGGER.fine("Schema: " + schemaFile.getAbsolutePath());

        if (!f.exists()) throw new LiftDocumentLoadingException("File does not exist: " + f.getAbsoluteFile());
        LOGGER.fine("Dictionary: " + f.getAbsolutePath());
    
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        SAXParser saxParser = null;
        try {
            saxParser = saxFactory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LiftFactory liftFactory = new LiftFactory();
        LiftSaxHandler lsh = new LiftSaxHandler(liftFactory);
        try {
            saxParser.parse(f, lsh);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        LiftDictionary d = new LiftDictionary(liftFactory.getLiftDictionaryCompoments());
        d.setLiftVersion(liftFactory.getLiftVersion());
        d.setLiftProducer(liftFactory.getLiftProducer());
        return d;
    }

}
