package fr.cnrs.lacito.liftapi.xml;

import java.util.Optional;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * Low-level helper for writing XML elements with a fluent API.
 * Wraps XMLStreamWriter and reduces boilerplate for common patterns.
 */
public class XmlElementWriter {
    private final XMLStreamWriter writer;

    public XmlElementWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    /**
     * Write an element with optional content.
     */
    public void writeElement(String name, ElementConsumer consumer) throws Exception {
        writer.writeStartElement(name);
        consumer.accept(this);
        writer.writeEndElement();
    }

    /**
     * Write an attribute (non-optional).
     */
    public void writeAttribute(String name, String value) throws XMLStreamException {
        if (value != null && !value.isEmpty()) {
            writer.writeAttribute(name, value);
        }
    }

    /**
     * Write an attribute from Optional.
     */
    public void writeAttribute(String name, Optional<String> opt) throws XMLStreamException {
        opt.ifPresent(value -> {
            try {
                writeAttribute(name, value);
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Write text content.
     */
    public void writeCharacters(String text) throws XMLStreamException {
        if (text != null && !text.isEmpty()) {
            writer.writeCharacters(text);
        }
    }

    /**
     * Get underlying writer for advanced operations.
     */
    public XMLStreamWriter getUnderlying() {
        return writer;
    }

    /**
     * Consumer interface for element content.
     */
    @FunctionalInterface
    public interface ElementConsumer {
        void accept(XmlElementWriter w) throws Exception;
    }
}
