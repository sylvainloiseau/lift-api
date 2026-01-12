package fr.cnrs.lacito.liftapi.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

// import com.pivovarit.function.ThrowingFunction;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryCompoments;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftHeaderFieldDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRange;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.Form;

public class LiftWriter  {

    private static final Logger LOGGER = Logger.getLogger(LiftWriter.class.getName());
    private final static String NEW_LINE = "\n";

    // Solution for throwing exception through lambda
    // https://4comprehension.com/sneakily-throwing-exceptions-in-lambda-expressions-in-java/
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
        @SuppressWarnings("unchecked")
        static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
            throw (T) t; // ( ͡° ͜ʖ ͡°)
        }       
    }

    static <T> Consumer<T> unchecked(ThrowingConsumer<T> f) {
        return t -> {
            try {
                f.accept(t);
            } catch (Exception ex) {
                ThrowingConsumer.sneakyThrow(ex);
            }
        };
    }

    public interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws Exception;
        @SuppressWarnings("unchecked")
        static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
            throw (T) t; // ( ͡° ͜ʖ ͡°)
        }       
    }

    static <T, U> BiConsumer<T, U> biunchecked(ThrowingBiConsumer<T, U> f) {
        return (t, u) -> {
            try {
                f.accept(t, u);
            } catch (Exception ex) {
                ThrowingConsumer.sneakyThrow(ex);
            }
        };
    }


    private XMLStreamWriter out = null;
    private OutputStream outputStream = null;

    public LiftWriter(File f) throws FileNotFoundException {
        outputStream = new FileOutputStream(f);
    }

    /**
     * Marshall the dictionary components to the output writer.
     * Exceptions are propagated to the caller.
     */
    public void marshall (LiftDictionary d) throws Exception {
        try {
            out = XMLOutputFactory.newInstance().createXMLStreamWriter(
                    new OutputStreamWriter(outputStream, "utf-8"));
        } catch (UnsupportedEncodingException | XMLStreamException | FactoryConfigurationError e) {
            e.printStackTrace();
        }

        LiftDictionaryCompoments c = d.getLiftDictionaryComponents();

        out.writeStartDocument(); // default "utf-8", "1.0"
        out.writeStartElement(LiftVocabulary.LIFT_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.VERSION_ATTRIBUTE, d.getLiftVersion());
        out.writeAttribute(LiftVocabulary.PRODUCER_ATTRIBUTE, d.getLiftProducer());

        LiftHeader header = c.getHeader();
        if (header != null) writeHeader(header);
        out.writeCharacters(NEW_LINE);

        List<LiftEntry> entries = c.getAllEntries();
        if (entries != null) {
            for (LiftEntry e : entries) if (e != null) writeEntry(e);
        }
        out.writeCharacters(NEW_LINE);

        out.writeEndElement(); // </lift>
        out.writeEndDocument();
        out.flush();
    }

    private void writeHeader(LiftHeader header) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_LOCAL_NAME);

        MultiText description = header.getDescription();
        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        if (description != null) writeMultiText(description);
        out.writeEndElement(); // LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME

        List<LiftHeaderRange> ranges = header.getRanges();
        if (ranges != null && !ranges.isEmpty()) {
            out.writeStartElement(LiftVocabulary.HEADER_RANGES_LOCAL_NAME);
            for (LiftHeaderRange r : ranges) {
                writeHeaderRange(r);
            }
            out.writeEndElement();
        }

        List<LiftHeaderFieldDefinition> fields = header.getFields();
        if (fields != null && !fields.isEmpty()) {
            out.writeStartElement(LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME);
            for (LiftHeaderFieldDefinition f : fields) {
                writeHeaderFieldDescription(f);
            }
            out.writeEndElement(); // LiftVocabulary.HEADER_FIELDS_DESCRIPTION_LOCAL_NAME
        }

        out.writeEndElement(); // LiftVocabulary.HEADER_LOCAL_NAME
    }

    /*
     * Description of a system of values
     */
    private void writeHeaderRange(LiftHeaderRange range) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, range.getId());
        if (range.getGuid().isPresent()) out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, range.getGuid().get());
        if (range.getHref().isPresent()) out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, range.getHref().get());
        writeAbstractExtensibleWithoutFieldProperties(range);
        writeAbstractExtensibleWithFieldProperties(range);

        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        writeMultiText(range.getDescription());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        writeMultiText(range.getLabel());
        out.writeEndElement();
        
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
        writeMultiText(range.getAbbrev());
        out.writeEndElement();

        // range elements
        List<LiftHeaderRangeElement> elems = range.getRangeElements();
            for (LiftHeaderRangeElement e : elems) {
                writeHeaderRangeElement(e);
            }

        out.writeEndElement(); // LiftVocabulary.HEADER_RANGE_LOCAL_NAME
    }

    /*
     * Description of a value in a system
     */
    private void writeHeaderRangeElement(LiftHeaderRangeElement el) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, el.getId());
        if (el.getGuid().isPresent()) out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, el.getGuid().get());
        writeAbstractExtensibleWithoutFieldProperties(el);
        writeAbstractExtensibleWithFieldProperties(el);

        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        writeMultiText(el.getDescription());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        writeMultiText(el.getLabel());
        out.writeEndElement();
        
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
        writeMultiText(el.getAbbrev());
        out.writeEndElement();

        out.writeEndElement(); // LiftVocabulary.HEADER_RANGE_ELEMENT_ELEMENT_LOCAL_NAME
    }
    
    private void writeHeaderFieldDescription(LiftHeaderFieldDefinition f) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME);
        
        out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, f.getName());
        if (f.getFClass().isPresent()) out.writeAttribute("class", f.getFClass().get());
        if (f.getType().isPresent()) out.writeAttribute("type", f.getType().get());
        if (f.getOptionRange().isPresent()) out.writeAttribute("option-range", f.getOptionRange().get());
        if (f.getWritingSystem().isPresent()) out.writeAttribute("writing-system", f.getWritingSystem().get());

        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        writeMultiText(f.getDescription());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        writeMultiText(f.getLabel());
        out.writeEndElement();

        out.writeEndElement(); // LiftVocabulary.HEADER_FIELD_DESCRIPTION_LOCAL_NAME
    }

    private void writeEntry(LiftEntry entry) throws Exception {
        out.writeStartElement(LiftVocabulary.ENTRY_LOCAL_NAME);
        if (entry.getDateDeleted().isPresent()) out.writeAttribute(LiftVocabulary.DATE_DELETED_ATTRIBUTE, entry.getDateDeleted().get());
        if (entry.getOrder().isPresent()) out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, entry.getOrder().get());
        writeAbstractIdentifiableProperties(entry);
        writeAbstractExtensibleWithoutFieldProperties(entry);
        writeAbstractNotableProperties(entry);
        writeAbstractExtensibleWithFieldProperties(entry);

        out.writeStartElement(LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME);
        writeMultiText(entry.getForms());
        out.writeEndElement();

        if (!entry.getCitations().isEmpty()) {
            out.writeStartElement(LiftVocabulary.CITATION_LOCAL_NAME);
            writeMultiText(entry.getCitations());
            out.writeEndElement();
        }
        
        entry.getPronunciations().forEach(unchecked(this::writePronunciation));
        entry.getVariants().forEach(unchecked(this::writeVariant));
        entry.getRelations().forEach(unchecked(this::writeRelation));
        entry.getEtymologies().forEach(unchecked(this::writeEtymology));
        entry.getSenses().forEach(unchecked(this::writeSense));

        out.writeEndElement();
    }

    private void writePronunciation(LiftPronunciation p) throws Exception {
        out.writeStartElement(LiftVocabulary.PRONUNCIATION_LOCAL_NAME);
        writeAbstractExtensibleWithoutFieldProperties(p);
        writeAbstractExtensibleWithFieldProperties(p);
        writeMultiText(p.getProunciation());
        p.getMedias().forEach(unchecked(this::writeMedia));
        out.writeEndElement();
    }

    private void writeMedia(LiftMedia m) throws Exception {
        out.writeStartElement(LiftVocabulary.MEDIA_LOCAL_NAME);
        out.writeAttribute("href", m.getHref());
        writeMultiText(m.getLabel());
        out.writeEndElement();
    }

    private void writeVariant(LiftVariant v) throws Exception {
        out.writeStartElement(LiftVocabulary.VARIANT_LOCAL_NAME);
        if (v.getRefId().isPresent()) out.writeAttribute("ref", v.getRefId().get());
        writeAbstractExtensibleWithoutFieldProperties(v);
        writeAbstractExtensibleWithFieldProperties(v);
        v.getPronunciations().forEach(unchecked(this::writePronunciation));
        v.getRelations().forEach(unchecked(this::writeRelation));
        writeMultiText(v.getForms());
        out.writeEndElement();
    }

    private void writeRelation(LiftRelation r) throws Exception {
        out.writeStartElement(LiftVocabulary.RELATION_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, r.getType());
        if (r.getRefID().isPresent()) out.writeAttribute(LiftVocabulary.REF_ATTRIBUTE, r.getRefID().get());
        if (r.getOrder().isPresent()) out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, r.getOrder().get().toString());
        writeAbstractExtensibleWithoutFieldProperties(r);
        writeAbstractExtensibleWithFieldProperties(r);
        writeMultiText(r.getUsage());
        out.writeEndElement();
    }

    private void writeEtymology(LiftEtymology e) throws Exception {
        out.writeStartElement(LiftVocabulary.ETYMOLOGY_LOCAL_NAME);
        if (e.getType() != null) out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, e.getType());
        if (e.getSource() != null) out.writeAttribute(LiftVocabulary.SOURCE_ATTRIBUTE, e.getSource());
        writeAbstractExtensibleWithoutFieldProperties(e);
        writeAbstractExtensibleWithFieldProperties(e);

        writeMultiText(e.getForms());
        writeMultiText(LiftVocabulary.GLOSS_LOCAL_NAME, e.getGlosses());
    
        out.writeEndElement();
    }

    private void writeSense(LiftSense sense) throws Exception {
        out.writeStartElement(LiftVocabulary.SENSE_LOCAL_NAME);
        if (sense.getOrder().isPresent()) out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, sense.getOrder().get().toString());
        writeAbstractIdentifiableProperties(sense);
        writeAbstractExtensibleWithoutFieldProperties(sense);
        writeAbstractNotableProperties(sense);
        writeAbstractExtensibleWithFieldProperties(sense);

        // TODO can have trait
        writeMultiText(LiftVocabulary.GLOSS_LOCAL_NAME, sense.getGloss());

        if (!sense.getDefinition().isEmpty()) {
            out.writeStartElement(LiftVocabulary.DEFINITION_LOCAL_NAME);
            writeMultiText(sense.getDefinition());
            out.writeEndElement();
        }

        if (sense.getGrammaticalInfo().isPresent())
            writeGrammaticalInfo(sense.getGrammaticalInfo().get());

        sense.getRelations().forEach(unchecked(this::writeRelation));
        sense.getExamples().forEach(unchecked(this::writeExample));
        sense.getIllustrations().forEach(unchecked(this::writeIllustration));
        sense.getSubSenses().forEach(unchecked(this::writeSense));

        out.writeEndElement();
    }

    private void writeGrammaticalInfo(GrammaticalInfo gi) throws Exception {
        out.writeStartElement(LiftVocabulary.GRAM_INFO_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, gi.getValue());
        gi.getTraits().forEach(unchecked(this::writeTrait));
        out.writeEndElement();
    }

    private void writeExample(LiftExample ex) throws Exception {
        out.writeStartElement(LiftVocabulary.EXAMPLE_LOCAL_NAME);
        writeAbstractExtensibleWithoutFieldProperties(ex);
        writeAbstractNotableProperties(ex);
        writeAbstractExtensibleWithFieldProperties(ex);

        ex.getTranslations().forEach(biunchecked((type, mt) -> {
                out.writeStartElement(LiftVocabulary.TRANSLATION_LOCAL_NAME);
                out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, type);
                writeMultiText(mt);
                out.writeEndElement();
        }));
        out.writeEndElement();
    }

    private void writeIllustration(LiftIllustration il) throws Exception {
        out.writeStartElement(LiftVocabulary.ILLUSTRATION_LOCAL_NAME);
        if (il.getHref() != null) out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, il.getHref());
        writeMultiText(il.getLabel());
        out.writeEndElement();
    }

    private void writeMultiText(MultiText mt) throws Exception {
        writeMultiText(LiftVocabulary.FORM_LOCAL_NAME, mt);
    }

    private void writeMultiText(String elementName, MultiText mt) throws Exception {
        Collection<Form> texts = mt.getForms();
        for (Form text : texts) {
            out.writeStartElement(elementName); // can be form or gloss
            out.writeAttribute(LiftVocabulary.LANG_ATTRIBUTE, text.getLang());
            out.writeStartElement(LiftVocabulary.TEXT_LOCAL_NAME);
            String txt = text.toString();
            // TODO : write the span content if any
            if (txt != null && !txt.isEmpty()) out.writeCharacters(txt);
            out.writeEndElement(); //text
            text.getAnnotations().forEach(unchecked(this::writeAnnotation));
            out.writeEndElement(); //form
        }
    }

    // Note and Trait writers use the generic helper
    private void writeNote(LiftNote n) throws Exception {
        out.writeStartElement(LiftVocabulary.NOTE_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, n.getType().orElse(""));
        writeAbstractExtensibleWithoutFieldProperties(n);
        writeAbstractExtensibleWithFieldProperties(n);
        writeMultiText(n.getText());
        out.writeEndElement();
    }

    private void writeTrait(LiftTrait t) throws Exception {
        out.writeStartElement(LiftVocabulary.TRAIT_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, t.getName());
        out.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, t.getValue());
        t.getAnnotations().forEach(unchecked(this::writeAnnotation));
        out.writeEndElement();
    }

    private void writeAnnotation(LiftAnnotation a) throws Exception {
        out.writeStartElement(LiftVocabulary.ANNOTATION_LOCAL_NAME);
        if (a.getName() != null) out.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, a.getName());
        if (a.getValue().isPresent()) out.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, a.getValue().get());
        if (a.getWho().isPresent()) out.writeAttribute(LiftVocabulary.WHO_ATTRIBUTE, a.getWho().get());
        if (a.getWhen().isPresent()) out.writeAttribute(LiftVocabulary.WHEN_ATTRIBUTE, a.getWhen().get());
        writeMultiText(a.getText());
        out.writeEndElement();
    }

    // TODO for all AbstractLiftRoot descendant classes, check the otherXmlAttributes object.

    private void writeAbstractExtensibleWithoutFieldProperties(AbstractExtensibleWithoutField obj) throws Exception {
        if (obj.getDateCreated().isPresent()) out.writeAttribute("dateCreated", obj.getDateCreated().get());
        if (obj.getDateModified().isPresent()) out.writeAttribute("dateModified", obj.getDateModified().get());
        obj.getAnnotations().forEach(unchecked(this::writeAnnotation));
        obj.getTraits().forEach(unchecked(this::writeTrait));
    }

    private void writeAbstractExtensibleWithFieldProperties(AbstractExtensibleWithField obj) throws Exception {
        obj.getFields().forEach(unchecked(this::writeField));
    }

    private void writeField(LiftField f) throws Exception {
        out.writeStartElement(LiftVocabulary.FIELD_LOCAL_NAME);
        writeAbstractExtensibleWithoutFieldProperties(f);
        writeMultiText(f.getText());
        out.writeEndElement();
    }

    private void writeAbstractIdentifiableProperties(AbstractIdentifiable obj) throws Exception {
        if (obj.getId().isPresent()) out.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, obj.getId().get());
        if (obj.getGuid().isPresent()) out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, obj.getGuid().get()); 
    }

    private void writeAbstractNotableProperties(AbstractNotable obj) throws Exception {
        obj.getNotes().values().forEach(unchecked(this::writeNote));
    }
}