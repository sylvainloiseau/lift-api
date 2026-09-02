package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Loads and parses an XML dictionary using SAX.
 */
public final class LiftDictionaryXmlReader {

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionaryXmlReader.class.getName()
    );

    private final File f;

    private final boolean validate;

    private LiftXMLFactoryNew liftFactory;

    private final LiftDictionary dictionary;

    /**
     * Constructs a LiftDictionaryXmlReader with the specified file, dictionary, and validation flag.
     *
     * @param f the XML file to parse
     * @param dictionary the dictionary to populate
     * @param validate whether to validate the XML against the schema
     */
    public LiftDictionaryXmlReader(
        File f,
        LiftDictionary dictionary,
        boolean validate
    ) {
        this.f = f;
        this.dictionary = dictionary;
        this.validate = validate;
    }

    public void parse() throws LiftDocumentLoadingException {
        // URL schemaUrl = LiftDictionaryLoader.class.getResource("schema/lift-0.13.xsd");
        // File schemaFile = new File(schemaUrl.getPath());
        // if (!schemaFile.exists()) throw new LiftDocumentLoadingException("Schema not found: " + schemaFile.getAbsoluteFile());
        // LOGGER.fine("Schema: " + schemaFile.getAbsolutePath());

        if (!f.exists()) throw new LiftDocumentLoadingException(
            "File does not exist: " + f.getAbsoluteFile()
        );
        LOGGER.fine("Dictionary: " + f.getAbsolutePath());

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        SAXParser saxParser = null;
        try {
            saxParser = saxFactory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.log(
                Level.SEVERE,
                "Unable to initialize SAX parser for file: " +
                    f.getAbsolutePath(),
                e
            );
            throw new LiftDocumentLoadingException(e);
        }

        this.liftFactory = new LiftXMLFactoryNew(dictionary);
        LiftSaxHandler lsh = new LiftSaxHandler(liftFactory);
        try {
            saxParser.parse(f, lsh);
        } catch (SAXException e) {
            LOGGER.log(
                Level.SEVERE,
                "Invalid XML while parsing LIFT file: " + f.getAbsolutePath(),
                e
            );
            throw new LiftDocumentLoadingException(e);
        } catch (IOException e) {
            LOGGER.log(
                Level.SEVERE,
                "I/O error while parsing LIFT file: " + f.getAbsolutePath(),
                e
            );
            throw new LiftDocumentLoadingException(e);
        } catch (DuplicateIdException e) {
            throw e;
        } catch (Exception exception) {
            LOGGER.log(
                Level.SEVERE,
                "Error while parsing LIFT file: " + f.getAbsolutePath(),
                exception
            );
            throw new LiftDocumentLoadingException(exception);
        }
    }

}
