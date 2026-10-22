package fr.cnrs.lacito.liftapi.xml;

import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The SAX parsers of the reader: one configuration for the {@code .lift} file and the
 * {@code .lift-ranges} files it refers to.
 *
 * LIFT files are routinely exchanged between FLEx users, so the input is untrusted: DTDs,
 * external entities and XInclude are disabled, which closes the XXE and entity-expansion
 * ("billion laughs") surface.
 */
final class LiftXmlParsers {

    private LiftXmlParsers() {}

    /** A namespace-aware SAX parser with DTDs, external entities and XInclude disabled. */
    static SAXParser newSecureParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        saxFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        saxFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        saxFactory.setXIncludeAware(false);
        SAXParser saxParser = saxFactory.newSAXParser();
        saxParser.getXMLReader().setEntityResolver((publicId, systemId) ->
            new InputSource(new StringReader(""))
        );
        return saxParser;
    }
}
