package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftVersion;

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
            w.writeAttribute(LiftVocabulary.DATE_CREATED_ATTRIBUTE, obj.getDateCreated().get());
        }
        if (obj.getDateModified().isPresent()) {
            w.writeAttribute(LiftVocabulary.DATE_MODIFIED_ATTRIBUTE, obj.getDateModified().get());
        }
        for (LiftAnnotation a : obj.getAnnotations()) {
            writeAnnotation(w, a);
        }
        for (LiftTrait t : obj.getTraits()) {
            writeTrait(w, t);
        }
    }

    public static void writeAbstractExtensibleWithField(
            XMLStreamWriter w,
            AbstractExtensibleWithField obj,
            LiftVersion version) throws Exception {
        for (LiftField f : obj.getFields().values()) {
            writeField(w, f, version);
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
            AbstractNotable obj,
            LiftVersion version) throws Exception {
        for (var entry : obj.getNotes().entrySet()) {
            writeNote(w, entry.getValue(), version);
        }
    }

    /**
     * Write an {@code <annotation>} element. This is the single implementation used
     * everywhere annotations are serialized.
     */
    public static void writeAnnotation(XMLStreamWriter w, LiftAnnotation a) throws Exception {
        w.writeStartElement(LiftVocabulary.ANNOTATION_LOCAL_NAME);
        if (a.getType() != null) {
            w.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, a.getType().getId());
        }
        if (!a.getValue().isEmpty()) {
            w.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, a.getValue());
        }
        if (!a.getWho().isEmpty()) {
            w.writeAttribute(LiftVocabulary.WHO_ATTRIBUTE, a.getWho());
        }
        if (!a.getWhen().isEmpty()) {
            w.writeAttribute(LiftVocabulary.WHEN_ATTRIBUTE, a.getWhen());
        }
        MultiTextWriters.writeMultiText(w, a.getText());
        w.writeEndElement();
    }

    /**
     * Write a {@code <trait>} element.
     */
    public static void writeTrait(XMLStreamWriter w, LiftTrait t) throws Exception {
        w.writeStartElement(LiftVocabulary.TRAIT_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, t.getSpecification().getName());
        w.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, t.getValue());
        for (LiftAnnotation a : t.getAnnotations()) {
            writeAnnotation(w, a);
        }
        w.writeEndElement();
    }

    /**
     * Write a {@code <field>} element.
     *
     * The name of the attribute naming the field definition changed between LIFT
     * versions: {@code field-content} declares {@code @type} in 0.13 and
     * {@code @name} in 0.15.
     */
    public static void writeField(XMLStreamWriter w, LiftField f, LiftVersion version) throws Exception {
        w.writeStartElement(LiftVocabulary.FIELD_LOCAL_NAME);
        w.writeAttribute(
            LiftVocabulary.fieldNameAttribute(version),
            f.getSpecification().getName()
        );
        writeAbstractExtensibleWithoutField(w, f);
        MultiTextWriters.writeMultiText(w, f.getText());
        w.writeEndElement();
    }

    /**
     * Write a {@code <note>} element.
     */
    public static void writeNote(
            XMLStreamWriter w,
            fr.cnrs.lacito.liftapi.model.LiftNote n,
            LiftVersion version) throws Exception {
        w.writeStartElement(LiftVocabulary.NOTE_LOCAL_NAME);
        if (n.getType() != null) {
            w.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, n.getType().getId());
        }
        writeAbstractExtensibleWithoutField(w, n);
        writeAbstractExtensibleWithField(w, n, version);
        MultiTextWriters.writeMultiText(w, n.getText());
        w.writeEndElement();
    }
}
