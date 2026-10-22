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

        SAXParser saxParser = null;
        try {
            saxParser = LiftXmlParsers.newSecureParser();
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.log(
                Level.SEVERE,
                "Unable to initialize SAX parser for file: " +
                    f.getAbsolutePath(),
                e
            );
            throw new LiftDocumentLoadingException(e);
        }

        LiftSaxHandler lsh = new LiftSaxHandler(dictionary, strict, f);
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
