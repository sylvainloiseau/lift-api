package fr.cnrs.lacito.liftapi.xml;

import java.util.List;
import org.xml.sax.Attributes;

/**
 * An element of a {@code .lift-ranges} file, as read: what the reader replays into the
 * header, as if the {@code <range>} of the {@code .lift} file held it (see
 * {@link LiftRangesFileReader}).
 *
 * @param localName the element's name
 * @param attributes a copy of its attributes
 * @param children its content in document order: {@code RecordedElement}s and the
 *        {@code String}s of its text
 * @param lineNumber the line of its start tag, for the messages of errors
 * @param columnNumber the column of its start tag
 */
record RecordedElement(
    String localName,
    Attributes attributes,
    List<Object> children,
    int lineNumber,
    int columnNumber
) {}
