/**
 * Classes for reading and writting the LIFT XML serialisation format
 * (<a href="https://github.com/sillsdev/lift-standard">https://github.com/sillsdev/lift-standard</a>).
 * 
 * Version 13 and 15 of the format are supported.
 *
 * <h2>Reading</h2>
 *
 * {@link fr.cnrs.lacito.liftapi.xml.LiftDictionaryXmlReader} parses the file with SAX;
 * {@code LiftSaxHandler} follows the structure of the document (the open elements, the
 * forms, their text and spans), and {@code LiftXmlDecoder} turns each element and its
 * attributes into calls to the builders of the {@code builder} package. The builders
 * are the only code that creates components: a file is loaded the way an application
 * builds a dictionary, through the same type resolution, value parsing and wiring. Only
 * what is specific to XML stays here: element and attribute names, the spellings that
 * differ between versions, the attributes LIFT does not define (kept for the round
 * trip), and the references a file may make before the declaration they point to,
 * resolved at the end of the header and of the document.
 *
 * <h2>Writing</h2>
 *
 * {@link fr.cnrs.lacito.liftapi.xml.LiftWriterSession} writes a dictionary, atomically:
 * to a temporary file first, promoted only on success.
 */
package fr.cnrs.lacito.liftapi.xml;
