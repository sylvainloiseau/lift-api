package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryComponents;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.LiftDocumentLoadingException;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasType;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.MultiText;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

public final class LiftDictionaryXmlReader {

    private static final Logger LOGGER = Logger.getLogger(
        LiftDictionaryXmlReader.class.getName()
    );
    private final File f;
    private final boolean validate;
    private LiftXMLFactoryNew liftFactory;
    private final LiftDictionaryRegistry registry;

    public LiftDictionaryXmlReader(
        File f,
        LiftDictionaryRegistry registry,
        boolean validate
    ) {
        this.f = f;
        this.registry = registry;
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

        this.liftFactory = new LiftXMLFactoryNew(registry);
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
        }
    }

    //public LiftDictionaryComponents getLiftDictionaryComponents() {
    //    return liftFactory.getLiftDictionaryComponents();
    //}

    public LiftHeader getHeader() {
        return liftFactory.getHeader();
    }

    public String getLiftVersion() {
        return liftFactory.getLiftVersion();
    }

    public String getLiftProducer() {
        return liftFactory.getLiftProducer();
    }
}
