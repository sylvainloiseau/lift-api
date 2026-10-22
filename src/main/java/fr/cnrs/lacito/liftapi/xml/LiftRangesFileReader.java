package fr.cnrs.lacito.liftapi.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads a {@code .lift-ranges} file: a {@code <lift-ranges>} root holding
 * {@code <range id="...">} elements.
 *
 * It does not decode anything: it records each range as a tree of
 * {@link RecordedElement}s, which {@link LiftSaxHandler} replays into the
 * {@code <range href="...">} of the header that refers to it, as if the range were written
 * there. So the content of a ranges file is read by the same code as a header - range
 * elements, their labels, abbreviations, descriptions, {@code parent}, guid, traits,
 * fields, annotations and spans - and cannot drift from it.
 *
 * Refused (an {@link IOException}): a root other than {@code lift-ranges}, a range
 * without an id, two ranges of the same id, and an {@code href} on a range (LIFT 0.13,
 * §21: the attribute may not be used within an external range definition file).
 */
final class LiftRangesFileReader {

    private static final Logger LOGGER = Logger.getLogger(LiftRangesFileReader.class.getName());

    /** How many files were parsed since the class was loaded: for the tests ("parsed once"). */
    static final AtomicInteger FILES_PARSED = new AtomicInteger();

    private LiftRangesFileReader() {}

    /**
     * Reads a ranges file.
     *
     * @return its ranges by id, in document order
     * @throws IOException if the file cannot be read, is not well-formed, or is not a
     *         valid ranges file
     */
    static Map<String, RecordedElement> read(Path file) throws IOException {
        LOGGER.fine(() -> "Reading the ranges file " + file);
        FILES_PARSED.incrementAndGet();
        Recorder recorder = new Recorder(file);
        try {
            LiftXmlParsers.newSecureParser().parse(file.toFile(), recorder);
        } catch (SAXException e) {
            throw new IOException("Invalid ranges file " + file + ": " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        return Collections.unmodifiableMap(recorder.ranges);
    }

    /** The SAX handler: records the ranges, checks the two top levels. */
    private static final class Recorder extends DefaultHandler {

        private final Path file;
        private Locator locator;
        private final Map<String, RecordedElement> ranges = new LinkedHashMap<>();
        /** The open elements below the root, innermost first. */
        private final Deque<RecordedElement> open = new ArrayDeque<>();
        private int depth;

        Recorder(Path file) {
            this.file = file;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            depth++;
            if (depth == 1) {
                if (!LiftVocabulary.LIFT_RANGES_ROOT.equals(localName)) {
                    throw error("the root element is <" + localName + ">, not <lift-ranges>");
                }
                return;
            }
            if (depth == 2) {
                if (!LiftVocabulary.HEADER_RANGE_LOCAL_NAME.equals(localName)) {
                    throw error("<lift-ranges> may only hold <range> elements, not <" + localName + ">");
                }
                String id = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.ID_ATTRIBUTE);
                if (id == null) {
                    throw error("a <range> has no id");
                }
                if (attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.HREF_ATTRIBUTE) != null) {
                    throw error("the range " + id + " has an href, which a ranges file may not use");
                }
                if (ranges.containsKey(id)) {
                    throw error("the range " + id + " is defined twice");
                }
            }
            RecordedElement element = new RecordedElement(
                localName,
                new AttributesImpl(attributes),
                new ArrayList<>(),
                locator == null ? -1 : locator.getLineNumber(),
                locator == null ? -1 : locator.getColumnNumber()
            );
            if (depth == 2) {
                ranges.put(element.attributes().getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.ID_ATTRIBUTE), element);
            } else {
                open.peek().children().add(element);
            }
            open.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (depth >= 2) {
                open.pop();
            }
            depth--;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (open.isEmpty()) {
                return;
            }
            List<Object> children = open.peek().children();
            String text = new String(ch, start, length);
            // The parser may split a text in several calls: keep one string.
            if (!children.isEmpty() && children.getLast() instanceof String previous) {
                children.set(children.size() - 1, previous + text);
            } else {
                children.add(text);
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        private SAXParseException error(String message) {
            return new SAXParseException(message + " (" + file.getFileName() + ")", locator);
        }
    }
}
