package fr.cnrs.lacito.liftapi.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryComponents;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRange;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;

/**
 * Manages XML serialization session with proper resource lifecycle.
 * Handles stream creation, closing, and high-level serialization operations.
 */
public class LiftWriterSession implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(LiftWriterSession.class.getName());
    private static final String NEW_LINE = "\n";

    private final File outputFile;
    private XMLStreamWriter out;
    private OutputStream outputStream;

    public LiftWriterSession(File outputFile) throws FileNotFoundException {
        this.outputFile = outputFile;
        try {
            this.outputStream = new FileOutputStream(outputFile);
        } catch (java.io.FileNotFoundException e) {
            throw e;
        }
    }

    /**
     * Marshall a dictionary to XML.
     */
    public void marshall(LiftDictionary d) throws Exception {
        try {
            initializeWriter();
            LiftDictionaryComponents c = d.getLiftDictionaryComponents();

            out.writeStartDocument();
            out.writeStartElement(LiftVocabulary.LIFT_LOCAL_NAME);
            out.writeAttribute(LiftVocabulary.VERSION_ATTRIBUTE, d.getLiftVersion());
            out.writeAttribute(LiftVocabulary.PRODUCER_ATTRIBUTE, d.getLiftProducer());

            LiftHeader header = c.getHeader();
            if (header != null) {
                writeHeader(header);
            }
            out.writeCharacters(NEW_LINE);
            if (header != null && header.getRanges() != null) {
                writeRangesToExternalFiles(header);
            }

            List<LiftEntry> entries = c.getAllEntries();
            if (entries != null) {
                for (LiftEntry e : entries) {
                    if (e != null) {
                        writeEntry(e);
                    }
                }
            }
            out.writeCharacters(NEW_LINE);

            out.writeEndElement(); // </lift>
            out.writeEndDocument();
            out.flush();
        } catch (UnsupportedEncodingException | XMLStreamException | FactoryConfigurationError e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Unable to initialize XML writer", e);
            throw e;
        }
    }

    /**
     * Close all resources properly.
     */
    @Override
    public void close() throws IOException {
        if (out != null) {
            try {
                out.close();
            } catch (XMLStreamException e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Error closing XML writer", e);
            }
        }
        if (outputStream != null) {
            outputStream.close();
        }
    }

    private void initializeWriter() throws XMLStreamException, UnsupportedEncodingException, FactoryConfigurationError {
        out = XMLOutputFactory.newInstance().createXMLStreamWriter(
                new OutputStreamWriter(outputStream, "utf-8"));
    }

    private void writeHeader(LiftHeader header) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_LOCAL_NAME);

        MultiText description = header.getDescription();
        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        if (description != null) {
            MultiTextWriters.writeMultiText(out, description);
        }
        out.writeEndElement();

        List<LiftHeaderRange> ranges = header.getRanges();
        if (ranges != null && !ranges.isEmpty()) {
            out.writeStartElement(LiftVocabulary.HEADER_RANGES_LOCAL_NAME);
            for (LiftHeaderRange r : ranges) {
                writeHeaderRange(r);
            }
            out.writeEndElement();
        }

        List<LiftFieldAndTraitDefinition> fields = header.getFields();
        if (fields != null && !fields.isEmpty()) {
            out.writeStartElement(LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME);
            for (LiftFieldAndTraitDefinition f : fields) {
                writeHeaderFieldDescription(f);
            }
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    private void writeHeaderRange(LiftHeaderRange range) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, range.getId());
        if (range.getGuid().isPresent()) {
            out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, range.getGuid().get());
        }
        if (range.getHref().isPresent()) {
            out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, range.getHref().get());
        }
        if (!range.getHref().isPresent()) {
            AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, range);
            AbstractPropertyWriters.writeAbstractExtensibleWithField(out, range);
            out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, range.getDescription());
            out.writeEndElement();
            out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, range.getLabel());
            out.writeEndElement();
            out.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, range.getAbbrev());
            out.writeEndElement();
            for (LiftHeaderRangeElement e : range.getRangeElements()) {
                writeHeaderRangeElement(e);
            }
        }
        out.writeEndElement();
    }

    private void writeHeaderRangeElement(LiftHeaderRangeElement el) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, el.getId());
        if (el.getParentId().isPresent()) {
            out.writeAttribute("parent", el.getParentId().get());
        }
        if (el.getGuid().isPresent()) {
            out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, el.getGuid().get());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, el);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, el);

        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, el.getDescription());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, el.getLabel());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, el.getAbbrev());
        out.writeEndElement();

        out.writeEndElement();
    }

    private void writeRangesToExternalFiles(LiftHeader header) throws Exception {
        if (outputFile == null) {
            return;
        }
        File baseDir = outputFile.getParentFile();
        if (baseDir == null) {
            baseDir = new File(".");
        }

        Map<File, List<LiftHeaderRange>> byHref = new LinkedHashMap<>();
        for (LiftHeaderRange r : header.getRanges()) {
            if (!r.getHref().isPresent()) {
                continue;
            }
            String href = r.getHref().get();
            if (href == null || href.isBlank()) {
                continue;
            }
            File targetFile = resolveHrefToFile(href, baseDir);
            byHref.computeIfAbsent(targetFile, k -> new java.util.ArrayList<>()).add(r);
        }

        for (Map.Entry<File, List<LiftHeaderRange>> e : byHref.entrySet()) {
            writeLiftRangesFile(e.getKey(), e.getValue());
        }
    }

    private File resolveHrefToFile(String href, File baseDir) {
        href = href.trim();
        if (href.startsWith("file:///")) {
            try {
                return new File(URI.create(href));
            } catch (Exception ex) {
                LOGGER.warning("Invalid file href: " + href);
            }
        }
        if (href.startsWith("file://")) {
            try {
                return new File(URI.create(href));
            } catch (Exception ex) {
                LOGGER.warning("Invalid file href: " + href);
            }
        }
        return new File(baseDir, href);
    }

    private void writeLiftRangesFile(File file, List<LiftHeaderRange> ranges) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8")) {
            XMLStreamWriter rangesOut = XMLOutputFactory.newInstance().createXMLStreamWriter(osw);
            rangesOut.writeStartDocument("utf-8", "1.0");
            rangesOut.writeCharacters(NEW_LINE);
            rangesOut.writeStartElement(LiftVocabulary.LIFT_RANGES_ROOT);
            rangesOut.writeCharacters(NEW_LINE);
            for (LiftHeaderRange r : ranges) {
                writeHeaderRangeToWriter(rangesOut, r);
            }
            rangesOut.writeEndElement();
            rangesOut.writeEndDocument();
            rangesOut.flush();
        }
    }

    private void writeHeaderRangeToWriter(XMLStreamWriter w, LiftHeaderRange range) throws Exception {
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, range.getId());
        if (range.getGuid().isPresent()) {
            w.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, range.getGuid().get());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(w, range);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(w, range);
        w.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, range.getDescription());
        w.writeEndElement();
        w.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, range.getLabel());
        w.writeEndElement();
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, range.getAbbrev());
        w.writeEndElement();
        for (LiftHeaderRangeElement el : range.getRangeElements()) {
            writeHeaderRangeElementToWriter(w, el);
        }
        w.writeEndElement();
        w.writeCharacters(NEW_LINE);
    }

    private void writeHeaderRangeElementToWriter(XMLStreamWriter w, LiftHeaderRangeElement el) throws Exception {
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, el.getId());
        if (el.getParentId().isPresent()) {
            w.writeAttribute("parent", el.getParentId().get());
        }
        if (el.getGuid().isPresent()) {
            w.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, el.getGuid().get());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(w, el);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(w, el);
        w.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, el.getDescription());
        w.writeEndElement();
        w.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, el.getLabel());
        w.writeEndElement();
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME);
        MultiTextWriters.writeMultiText(w, el.getAbbrev());
        w.writeEndElement();
        w.writeEndElement();
        w.writeCharacters(NEW_LINE);
    }

    private void writeHeaderFieldDescription(LiftFieldAndTraitDefinition f) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, f.getName());
        if (f.getFClass().isPresent()) {
            out.writeAttribute("class", f.getFClass().get());
        }
        if (f.getType().isPresent()) {
            out.writeAttribute("type", f.getType().get());
        }
        if (f.getOptionRange().isPresent()) {
            out.writeAttribute("option-range", f.getOptionRange().get());
        }
        if (f.getWritingSystem().isPresent()) {
            out.writeAttribute("writing-system", f.getWritingSystem().get());
        }

        out.writeStartElement(LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, f.getDescription());
        out.writeEndElement();

        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, f.getLabel());
        out.writeEndElement();

        out.writeEndElement();
    }

    private void writeEntry(LiftEntry entry) throws Exception {
        out.writeStartElement(LiftVocabulary.ENTRY_LOCAL_NAME);
        if (entry.getDateDeleted().isPresent()) {
            out.writeAttribute(LiftVocabulary.DATE_DELETED_ATTRIBUTE, entry.getDateDeleted().get());
        }
        if (entry.getOrder().isPresent()) {
            out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, entry.getOrder().get());
        }
        AbstractPropertyWriters.writeAbstractIdentifiable(out, entry);
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, entry);
        AbstractPropertyWriters.writeAbstractNotable(out, entry);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, entry);

        out.writeStartElement(LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, entry.getForms());
        out.writeEndElement();

        if (!entry.getCitations().isEmpty()) {
            out.writeStartElement(LiftVocabulary.CITATION_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, entry.getCitations());
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
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, p);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, p);
        MultiTextWriters.writeMultiText(out, p.getPronunciation());
        p.getMedias().forEach(unchecked(this::writeMedia));
        out.writeEndElement();
    }

    private void writeMedia(LiftMedia m) throws Exception {
        out.writeStartElement(LiftVocabulary.MEDIA_LOCAL_NAME);
        out.writeAttribute("href", m.getHref());
        MultiTextWriters.writeMultiText(out, m.getLabel());
        out.writeEndElement();
    }

    private void writeVariant(LiftVariant v) throws Exception {
        out.writeStartElement(LiftVocabulary.VARIANT_LOCAL_NAME);
        if (v.getRefId().isPresent()) {
            out.writeAttribute("ref", v.getRefId().get());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, v);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, v);
        v.getPronunciations().forEach(unchecked(this::writePronunciation));
        v.getRelations().forEach(unchecked(this::writeRelation));
        MultiTextWriters.writeMultiText(out, v.getForms());
        out.writeEndElement();
    }

    private void writeRelation(LiftRelation r) throws Exception {
        out.writeStartElement(LiftVocabulary.RELATION_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, r.getType());
        if (r.getRefID().isPresent()) {
            out.writeAttribute(LiftVocabulary.REF_ATTRIBUTE, r.getRefID().get());
        }
        if (r.getOrder().isPresent()) {
            out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, r.getOrder().get().toString());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, r);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, r);
        if (!r.getUsage().isEmpty()) {
            out.writeStartElement(LiftVocabulary.USAGE_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, r.getUsage());
            out.writeEndElement();
        }
        out.writeEndElement();
    }

    private void writeReversal(LiftReversal rev) throws Exception {
        out.writeStartElement(LiftVocabulary.REVERSAL_LOCAL_NAME);
        if (rev.getType().isPresent()) {
            out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, rev.getType().get());
        }
        MultiTextWriters.writeMultiText(out, rev.getForms());
        if (rev.getMain() != null) {
            out.writeStartElement(LiftVocabulary.MAIN_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, rev.getMain().getForms());
            if (rev.getMain().getMain() != null) {
                writeReversal(rev.getMain());
            }
            out.writeEndElement();
        }
        out.writeEndElement();
    }

    private void writeEtymology(LiftEtymology e) throws Exception {
        out.writeStartElement(LiftVocabulary.ETYMOLOGY_LOCAL_NAME);
        if (e.getType() != null) {
            out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, e.getType());
        }
        if (e.getSource() != null) {
            out.writeAttribute(LiftVocabulary.SOURCE_ATTRIBUTE, e.getSource());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, e);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, e);

        MultiTextWriters.writeMultiText(out, e.getForms());
        MultiTextWriters.writeMultiText(out, LiftVocabulary.GLOSS_LOCAL_NAME, e.getGlosses());

        out.writeEndElement();
    }

    private void writeSense(LiftSense sense) throws Exception {
        out.writeStartElement(LiftVocabulary.SENSE_LOCAL_NAME);
        if (sense.getOrder().isPresent()) {
            out.writeAttribute(LiftVocabulary.ORDER_ATTRIBUTE, sense.getOrder().get().toString());
        }
        AbstractPropertyWriters.writeAbstractIdentifiable(out, sense);
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, sense);
        AbstractPropertyWriters.writeAbstractNotable(out, sense);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, sense);

        MultiTextWriters.writeMultiText(out, LiftVocabulary.GLOSS_LOCAL_NAME, sense.getGloss());

        if (!sense.getDefinition().isEmpty()) {
            out.writeStartElement(LiftVocabulary.DEFINITION_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, sense.getDefinition());
            out.writeEndElement();
        }

        if (sense.getGrammaticalInfo().isPresent()) {
            writeGrammaticalInfo(sense.getGrammaticalInfo().get());
        }

        sense.getRelations().forEach(unchecked(this::writeRelation));
        sense.getExamples().forEach(unchecked(this::writeExample));
        sense.getIllustrations().forEach(unchecked(this::writeIllustration));
        sense.getReversals().forEach(unchecked(this::writeReversal));
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
        if (ex.getSource().isPresent()) {
            String s = ex.getSource().get();
            if (s != null && !s.isBlank()) {
                out.writeAttribute(LiftVocabulary.SOURCE_ATTRIBUTE, s);
            }
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, ex);
        AbstractPropertyWriters.writeAbstractNotable(out, ex);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, ex);

        MultiTextWriters.writeMultiText(out, ex.getExample());

        ex.getTranslations().forEach(biunchecked((type, mt) -> {
            out.writeStartElement(LiftVocabulary.TRANSLATION_LOCAL_NAME);
            out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, type);
            MultiTextWriters.writeMultiText(out, mt);
            out.writeEndElement();
        }));
        out.writeEndElement();
    }

    private void writeIllustration(LiftIllustration il) throws Exception {
        out.writeStartElement(LiftVocabulary.ILLUSTRATION_LOCAL_NAME);
        if (il.getHref() != null) {
            out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, il.getHref());
        }
        MultiTextWriters.writeMultiText(out, il.getLabel());
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
        if (a.getName() != null) {
            out.writeAttribute(LiftVocabulary.NAME_ATTRIBUTE, a.getName());
        }
        if (a.getValue().isPresent()) {
            out.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, a.getValue().get());
        }
        if (a.getWho().isPresent()) {
            out.writeAttribute(LiftVocabulary.WHO_ATTRIBUTE, a.getWho().get());
        }
        if (a.getWhen().isPresent()) {
            out.writeAttribute(LiftVocabulary.WHEN_ATTRIBUTE, a.getWhen().get());
        }
        MultiTextWriters.writeMultiText(out, a.getText());
        out.writeEndElement();
    }

    // Helper for unchecked exceptions in lambdas
    private static <T> java.util.function.Consumer<T> unchecked(ThrowingConsumer<T> f) {
        return t -> {
            try {
                f.accept(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private static <T, U> java.util.function.BiConsumer<T, U> biunchecked(ThrowingBiConsumer<T, U> f) {
        return (t, u) -> {
            try {
                f.accept(t, u);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws Exception;
    }
}
