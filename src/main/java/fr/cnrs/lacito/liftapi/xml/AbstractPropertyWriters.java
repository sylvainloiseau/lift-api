package fr.cnrs.lacito.liftapi.xml;

import javax.xml.stream.XMLStreamWriter;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftField;

/**
 * Static helpers for writing common model properties to XML.
 * Eliminates duplication between write* and write*ToWriter method pairs.
 */
public class AbstractPropertyWriters {

    public static void writeAbstractExtensibleWithoutField(
            XMLStreamWriter w, 
            AbstractExtensibleWithoutField obj) throws Exception {
        if (obj.getDateCreated().isPresent()) {
            w.writeAttribute("dateCreated", obj.getDateCreated().get());
        }
        if (obj.getDateModified().isPresent()) {
            w.writeAttribute("dateModified", obj.getDateModified().get());
        }
        for (LiftAnnotation a : obj.getAnnotations()) {
            writeAnnotationElement(w, a);
        }
        for (LiftTrait t : obj.getTraits()) {
            writeTraitElement(w, t);
        }
    }

    public static void writeAbstractExtensibleWithField(
            XMLStreamWriter w, 
            AbstractExtensibleWithField obj) throws Exception {
        for (LiftField f : obj.getFields()) {
            writeFieldElement(w, f);
        }
    }

    public static void writeAbstractIdentifiable(
            XMLStreamWriter w, 
            AbstractIdentifiable obj) throws Exception {
        if (obj.getId().isPresent()) {
            w.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, obj.getId().get());
        }
        if (obj.getGuid().isPresent()) {
            w.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, obj.getGuid().get());
        }
    }

    public static void writeAbstractNotable(
            XMLStreamWriter w, 
            AbstractNotable obj) throws Exception {
        for (var entry : obj.getNotes().entrySet()) {
            writeNoteElement(w, entry.getValue());
        }
    }

    private static void writeAnnotationElement(XMLStreamWriter w, LiftAnnotation a) throws Exception {
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
        MultiTextWriters.writeMultiText(w, a.getText());
        w.writeEndElement();
    }

    private static void writeTraitElement(XMLStreamWriter w, LiftTrait t) throws Exception {
        w.writeStartElement(LiftVocabulary.TRAIT_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, t.getName());
        w.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, t.getValue());
        for (LiftAnnotation a : t.getAnnotations()) {
            writeAnnotationElement(w, a);
        }
        w.writeEndElement();
    }

    private static void writeFieldElement(XMLStreamWriter w, LiftField f) throws Exception {
        w.writeStartElement(LiftVocabulary.FIELD_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, f.getName());
        writeAbstractExtensibleWithoutField(w, f);
        MultiTextWriters.writeMultiText(w, f.getText());
        w.writeEndElement();
    }

    private static void writeNoteElement(XMLStreamWriter w, fr.cnrs.lacito.liftapi.model.LiftNote n) throws Exception {
        w.writeStartElement(LiftVocabulary.NOTE_LOCAL_NAME);
        if (n.getType().isPresent()) {
            w.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, n.getType().get());
        }
        writeAbstractExtensibleWithoutField(w, n);
        writeAbstractExtensibleWithField(w, n);
        MultiTextWriters.writeMultiText(w, n.getText());
        w.writeEndElement();
    }
}
