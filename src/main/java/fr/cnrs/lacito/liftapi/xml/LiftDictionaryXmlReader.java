package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Loads and parses an XML dictionary using SAX.
 */
public final class LiftDictionaryXmlReader {

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionaryXmlReader.class.getName()
    );

    private final File f;

    /**
     * When {@code true}, recoverable XML errors reported by the parser abort the
     * load instead of only being logged.
     */
    private final boolean strict;

    private LiftXMLFactory liftFactory;

    private final LiftDictionary dictionary;

    /**
     * Constructs a LiftDictionaryXmlReader with the specified file, dictionary, and validation flag.
     *
     * @param f the XML file to parse
     * @param dictionary the dictionary to populate
     * @param strict whether recoverable XML errors should abort the load
     */
    public LiftDictionaryXmlReader(
        File f,
        LiftDictionary dictionary,
        boolean strict
    ) {
        this.f = f;
        this.dictionary = dictionary;
        this.strict = strict;
    }

    public void parse() throws LiftDocumentLoadingException {
        if (!f.exists()) throw new LiftDocumentLoadingException(
            "File does not exist: " + f.getAbsoluteFile()
        );
        LOGGER.fine("Dictionary: " + f.getAbsolutePath());

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        SAXParser saxParser = null;
        try {
            // LIFT files are routinely exchanged between FLEx users, so the input is
            // untrusted: disable DTDs, external entities and XInclude to close the
            // XXE and entity-expansion ("billion laughs") surface.
            saxFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
            );
            saxFactory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
            );
            saxFactory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
            );
            saxFactory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
            );
            saxFactory.setXIncludeAware(false);
            saxParser = saxFactory.newSAXParser();
            saxParser
                .getXMLReader()
                .setEntityResolver((publicId, systemId) ->
                    new InputSource(new StringReader(""))
                );
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.log(
                Level.SEVERE,
                "Unable to initialize SAX parser for file: " +
                    f.getAbsolutePath(),
                e
            );
            throw new LiftDocumentLoadingException(e);
        }

        this.liftFactory = new LiftXMLFactory(dictionary);
        LiftSaxHandler lsh = new LiftSaxHandler(liftFactory, strict);
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
