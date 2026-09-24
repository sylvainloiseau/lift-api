package fr.cnrs.lacito.liftapi.xml;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;

/**
 * Static helpers for writing MultiText and related structures to XML.
 */
public class MultiTextWriters {

    private static final Logger LOGGER = Logger.getLogger(
        MultiTextWriters.class.getName()
    );

    /**
     * Write MultiText as default form elements.
     */
    public static void writeMultiText(XMLStreamWriter w, MultiText mt) throws Exception {
        writeMultiText(w, LiftVocabulary.FORM_LOCAL_NAME, mt);
    }

    /**
     * Write MultiText with custom element name (form, gloss, etc.).
     *
     * {@code multitext-content} is {@code form*}: annotations held by the MultiText
     * itself have no place of their own, so they are written inside the first form
     * (where {@code form-no-lang-content} does allow them). They are read back as
     * annotations of that form.
     */
    public static void writeMultiText(XMLStreamWriter w, String elementName, MultiText mt) throws Exception {
        if (mt == null) {
            return;
        }

        // Forms live in a hash map, so sort by language to make the output reproducible.
        List<Form> forms = new ArrayList<>(mt.getForms());
        forms.sort(Comparator.comparing(Form::getLang));

        if (forms.isEmpty() && !mt.getAnnotations().isEmpty()) {
            LOGGER.warning(
                "Writing " +
                    mt.getAnnotations().size() +
                    " annotation(s) outside any <form>: the MultiText has no form to " +
                    "host them, so the output is not valid against the LIFT schema."
            );
            for (LiftAnnotation ann : mt.getAnnotations()) {
                AbstractPropertyWriters.writeAnnotation(w, ann);
            }
            return;
        }

        boolean firstForm = true;
        for (Form text : forms) {
            w.writeStartElement(elementName);
            w.writeAttribute(LiftVocabulary.LANG_ATTRIBUTE, text.getLang());
            w.writeStartElement(LiftVocabulary.TEXT_LOCAL_NAME);
            writeTextSpanChildren(w, text.getTextSpanRoot());
            w.writeEndElement(); // text

            if (firstForm) {
                for (LiftAnnotation ann : mt.getAnnotations()) {
                    AbstractPropertyWriters.writeAnnotation(w, ann);
                }
                firstForm = false;
            }
            for (LiftAnnotation ann : text.getAnnotations()) {
                AbstractPropertyWriters.writeAnnotation(w, ann);
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
                        w.writeAttribute(LiftVocabulary.CLASS_ATTRIBUTE, child.getSClass().get());
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
}
