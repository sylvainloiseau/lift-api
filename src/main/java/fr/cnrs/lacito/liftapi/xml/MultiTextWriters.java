package fr.cnrs.lacito.liftapi.xml;

import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;

/**
 * Static helpers for writing MultiText and related structures to XML.
 */
public class MultiTextWriters {

    /**
     * Write MultiText as default form elements.
     */
    public static void writeMultiText(XMLStreamWriter w, MultiText mt) throws Exception {
        writeMultiText(w, LiftVocabulary.FORM_LOCAL_NAME, mt);
    }

    /**
     * Write MultiText with custom element name (form, gloss, etc.).
     */
    public static void writeMultiText(XMLStreamWriter w, String elementName, MultiText mt) throws Exception {
        if (mt == null) {
            return;
        }
        
        // Write annotations at the multitext level
        for (LiftAnnotation ann : mt.getAnnotations()) {
            writeAnnotation(w, ann);
        }
        
        // Write each form
        Collection<Form> texts = mt.getForms();
        for (Form text : texts) {
            w.writeStartElement(elementName);
            w.writeAttribute(LiftVocabulary.LANG_ATTRIBUTE, text.getLang());
            w.writeStartElement(LiftVocabulary.TEXT_LOCAL_NAME);
            writeTextSpanChildren(w, text.getTextSpanRoot());
            w.writeEndElement(); // text
            
            // Write form-level annotations
            for (LiftAnnotation ann : text.getAnnotations()) {
                writeAnnotation(w, ann);
            }
            w.writeEndElement(); // form/gloss
        }
    }

    /**
     * Recursively write TextSpan children.
     * For the root TextSpan (no attributes, acts as wrapper),
     * writes only its children without wrapping span element.
     */
    public static void writeTextSpanChildren(XMLStreamWriter w, TextSpan span) throws Exception {
        if (span.isTerminal()) {
            String text = span.getTerminalText();
            if (text != null && !text.isEmpty()) {
                w.writeCharacters(text);
            }
        } else {
            for (TextSpan child : span.getInnerContent()) {
                if (child.isTerminal()) {
                    String text = child.getTerminalText();
                    if (text != null && !text.isEmpty()) {
                        w.writeCharacters(text);
                    }
                } else {
                    w.writeStartElement(LiftVocabulary.SPAN_LOCAL_NAME);
                    if (child.getLang().isPresent()) {
                        w.writeAttribute(LiftVocabulary.LANG_ATTRIBUTE, child.getLang().get());
                    }
                    if (child.getSClass().isPresent()) {
                        w.writeAttribute("class", child.getSClass().get());
                    }
                    if (child.getHref().isPresent()) {
                        w.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, child.getHref().get());
                    }
                    writeTextSpanChildren(w, child);
                    w.writeEndElement(); // span
                }
            }
        }
    }

    /**
     * Write a single form (used in header ranges and other contexts).
     */
    public static void writeForm(XMLStreamWriter w, Form form) throws Exception {
        w.writeStartElement(LiftVocabulary.FORM_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.LANG_ATTRIBUTE, form.getLang());
        w.writeStartElement(LiftVocabulary.TEXT_LOCAL_NAME);
        w.writeCharacters(form.toPlainText() != null ? form.toPlainText() : "");
        w.writeEndElement();
        w.writeEndElement();
    }

    /**
     * Write a LiftAnnotation element.
     */
    private static void writeAnnotation(XMLStreamWriter w, LiftAnnotation a) throws Exception {
        w.writeStartElement(LiftVocabulary.ANNOTATION_LOCAL_NAME);
        if (a.getName() != null) {
            w.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, a.getName());
        }
        if (a.getValue().isPresent()) {
            w.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, a.getValue().get());
        }
        if (a.getWho().isPresent()) {
            w.writeAttribute(LiftVocabulary.WHO_ATTRIBUTE, a.getWho().get());
        }
        if (a.getWhen().isPresent()) {
            w.writeAttribute(LiftVocabulary.WHEN_ATTRIBUTE, a.getWhen().get());
        }
        writeMultiText(w, a.getText());
        w.writeEndElement();
    }
}
