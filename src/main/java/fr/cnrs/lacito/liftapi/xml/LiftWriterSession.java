package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftVersion;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.MultiText;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Facade for serializing a LiftDictionary to XML.
 *
 * Manages XML serialization session with proper resource lifecycle.
 *
 * The document is written to a temporary file next to the requested output and
 * moved into place by {@link #close()} only once {@link #marshall(LiftDictionary)}
 * has completed. A failure part way through therefore leaves any pre-existing
 * file untouched instead of destroying it.
 *
 * Use {@link LiftDictionary#save()} or call directly:
 *
 * <pre>
 *   LiftDictionary dict = LiftDictionary.loadDictionaryWithFile(inputFile);
 *   // ... modify dictionary ...
 *   try (LiftWriterSession session = new LiftWriterSession(outputFile)) {
 *       session.marshall(dict);
 *   }
 * </pre>
 *
 * Handles stream creation, closing, and high-level serialization operations.
 */
public class LiftWriterSession implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(
        LiftWriterSession.class.getName()
    );
    private static final String NEW_LINE = "\n";
    private static final String XML_VERSION = "1.0";

    private final File outputFile;
    private final Path temporaryFile;
    private XMLStreamWriter out;
    private OutputStream outputStream;

    /**
     * Whether the document was serialized in full. Only then is the temporary
     * file promoted to {@link #outputFile}.
     */
    private boolean complete = false;

    /** The LIFT version being written; several elements are spelled differently per version. */
    private LiftVersion version = LiftVersion.V0_15;

    public LiftWriterSession(File outputFile) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile cannot be null");
        }
        this.outputFile = outputFile;
        Path target = outputFile.toPath().toAbsolutePath();
        Path directory = target.getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            throw new FileNotFoundException(
                "Output directory does not exist: " + directory
            );
        }
        this.temporaryFile = Files.createTempFile(
            directory,
            target.getFileName().toString(),
            ".tmp"
        );
        this.outputStream = Files.newOutputStream(temporaryFile);
    }

    /**
     * Marshall a dictionary to XML.
     */
    public void marshall(LiftDictionary d) throws Exception {
        try {
            initializeWriter();

            out.writeStartDocument(
                StandardCharsets.UTF_8.name(),
                XML_VERSION
            );
            this.version = d.getLiftVersion();
            out.writeStartElement(LiftVocabulary.LIFT_LOCAL_NAME);
            out.writeAttribute(
                LiftVocabulary.VERSION_ATTRIBUTE,
                LiftVocabulary.versionAttributeValue(version)
            );
            out.writeAttribute(
                LiftVocabulary.PRODUCER_ATTRIBUTE,
                d.getLiftProducer()
            );

            LiftHeader header = d.getHeader();
            if (header != null) {
                writeHeader(header);
            }
            out.writeCharacters(NEW_LINE);
            if (header != null && header.getFeatureSets() != null) {
                writeRangesToExternalFiles(header);
            }

            List<LiftEntry> entries = d.getLiftDictionaryRegistry().getEntries();
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
            complete = true;
        } catch (XMLStreamException | FactoryConfigurationError e) {
            LOGGER.log(
                java.util.logging.Level.SEVERE,
                "Unable to write the LIFT document",
                e
            );
            throw e;
        }
    }

    /**
     * Close all resources and, if the document was written in full, replace the
     * output file with it.
     *
     * A failure to flush or close is reported rather than logged and dropped: a
     * lost final flush is lost data.
     */
    @Override
    public void close() throws IOException {
        IOException failure = null;
        if (out != null) {
            try {
                out.close();
            } catch (XMLStreamException e) {
                complete = false;
                failure = new IOException("Error closing XML writer", e);
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                complete = false;
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }

        try {
            if (complete) {
                commit();
            } else {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (IOException e) {
            if (failure == null) failure = e;
            else failure.addSuppressed(e);
        }

        if (failure != null) throw failure;
    }

    private void commit() throws IOException {
        Path target = outputFile.toPath().toAbsolutePath();
        try {
            Files.move(
                temporaryFile,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException e) {
            // Some filesystems (and cross-device moves) cannot do this atomically.
            Files.move(
                temporaryFile,
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void initializeWriter()
        throws XMLStreamException, FactoryConfigurationError {
        out = XMLOutputFactory.newInstance().createXMLStreamWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        );
    }

    private void writeHeader(LiftHeader header) throws Exception {
        out.writeStartElement(LiftVocabulary.HEADER_LOCAL_NAME);

        writeMultiTextElement(
            out,
            LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME,
            header.getDescription()
        );

        List<FeatureSet> ranges = header.getFeatureSets();
        if (ranges != null && !ranges.isEmpty()) {
            out.writeStartElement(LiftVocabulary.HEADER_RANGES_LOCAL_NAME);
            for (FeatureSet r : ranges) {
                writeHeaderRange(out, r, true);
            }
            out.writeEndElement();
        }

        Collection<LiftFieldAndTraitDefinition> fields =
            header.getFieldsAndTraitsDefinitions();
        if (fields != null && !fields.isEmpty()) {
            out.writeStartElement(
                LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME
            );
            for (LiftFieldAndTraitDefinition f : fields) {
                writeHeaderFieldDescription(f);
            }
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * Write a {@code <range>}.
     *
     * The same method serves the header and the external {@code .lift-ranges} file;
     * they used to be two copies that had already drifted apart (the copy for the
     * ranges file had lost the {@code href} handling).
     *
     * @param inHeader {@code true} inside {@code <header><ranges>}, where a range with
     *        an {@code href} is only a reference and carries no content
     */
    private void writeHeaderRange(
        XMLStreamWriter w,
        FeatureSet range,
        boolean inHeader
    ) throws Exception {
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, range.getId());
        if (range.getGuid().isPresent()) {
            w.writeAttribute(
                LiftVocabulary.GUID_ATTRIBUTE,
                range.getGuid().get()
            );
        }
        boolean external = range.getHref().isPresent();
        if (inHeader && external) {
            w.writeAttribute(
                LiftVocabulary.HREF_ATTRIBUTE,
                range.getHref().get()
            );
        }
        if (!inHeader || !external) {
            AbstractPropertyWriters.writeAbstractExtensibleWithoutField(w, range);
            AbstractPropertyWriters.writeAbstractExtensibleWithField(
                w,
                range,
                version
            );
            writeMultiTextElement(
                w,
                LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME,
                range.getDescription()
            );
            writeMultiTextElement(
                w,
                LiftVocabulary.LABEL_LOCAL_NAME,
                range.getLabel()
            );
            writeMultiTextElement(
                w,
                LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME,
                range.getAbbrev()
            );
            for (Feature e : range.getFeatures().values()) {
                writeHeaderRangeElement(w, e);
            }
        }
        w.writeEndElement();
    }

    private void writeHeaderRangeElement(XMLStreamWriter w, Feature el)
        throws Exception {
        w.writeStartElement(LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME);
        w.writeAttribute(LiftVocabulary.ID_ATTRIBUTE, el.getId());
        if (el.getSuperOrdinateFeature().isPresent()) {
            w.writeAttribute(
                LiftVocabulary.PARENT_ATTRIBUTE,
                el.getSuperOrdinateFeature().get().getId()
            );
        }
        if (el.getGuid().isPresent()) {
            w.writeAttribute(LiftVocabulary.GUID_ATTRIBUTE, el.getGuid().get());
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(w, el);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(w, el, version);

        writeMultiTextElement(
            w,
            LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME,
            el.getDescription()
        );
        writeMultiTextElement(w, LiftVocabulary.LABEL_LOCAL_NAME, el.getLabel());
        writeMultiTextElement(
            w,
            LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME,
            el.getAbbrev()
        );

        w.writeEndElement();
    }

    /**
     * Write {@code <name>multitext</name>}, or nothing when the multitext is empty.
     *
     * description, label and abbrev are all optional in {@code range-content} and
     * {@code range-element-content}; emitting them empty only adds noise.
     */
    private static void writeMultiTextElement(
        XMLStreamWriter w,
        String elementName,
        MultiText mt
    ) throws Exception {
        if (mt == null || (mt.isEmpty() && mt.getAnnotations().isEmpty())) return;
        w.writeStartElement(elementName);
        MultiTextWriters.writeMultiText(w, mt);
        w.writeEndElement();
    }

    private void writeRangesToExternalFiles(LiftHeader header)
        throws Exception {
        File baseDir = outputFile.getAbsoluteFile().getParentFile();
        if (baseDir == null) {
            return;
        }

        Map<File, List<FeatureSet>> byHref = new LinkedHashMap<>();
        for (FeatureSet r : header.getFeatureSets()) {
            if (!r.getHref().isPresent()) {
                continue;
            }
            String href = r.getHref().get();
            if (href == null || href.isBlank()) {
                continue;
            }
            if (!r.isExternalContentLoaded()) {
                LOGGER.info(
                    "Not rewriting the external ranges file of range '" +
                        r.getId() +
                        "' (" +
                        href +
                        "): its content was never loaded."
                );
                continue;
            }
            File targetFile = resolveHrefToFile(href, baseDir);
            byHref
                .computeIfAbsent(targetFile, k -> new java.util.ArrayList<>())
                .add(r);
        }

        for (Map.Entry<File, List<FeatureSet>> e : byHref.entrySet()) {
            writeLiftRangesFile(e.getKey(), e.getValue());
        }
        // TODO write Range that are not external
    }

    /**
     * Resolve an {@code @href} taken from document content to a file inside the
     * dictionary directory.
     *
     * The href comes from the document being saved, so it is untrusted: a
     * {@code ../../..} path or an absolute {@code file:} URI would otherwise turn
     * into a truncation target anywhere on the filesystem.
     *
     * @throws IOException if the href does not resolve inside {@code baseDir}
     */
    private File resolveHrefToFile(String href, File baseDir)
        throws IOException {
        href = href.trim();
        Path base = baseDir.toPath().toAbsolutePath().normalize();
        Path resolved;
        if (href.startsWith("file:/")) {
            try {
                resolved = Path.of(URI.create(href)).toAbsolutePath().normalize();
            } catch (IllegalArgumentException | java.nio.file.FileSystemNotFoundException ex) {
                throw new IOException("Invalid file href: " + href, ex);
            }
        } else {
            resolved = base.resolve(href).normalize();
        }
        if (!resolved.startsWith(base)) {
            throw new IOException(
                "Refusing to write a ranges file outside the dictionary directory: " +
                    href +
                    " resolves to " +
                    resolved
            );
        }
        return resolved.toFile();
    }

    private void writeLiftRangesFile(File file, List<FeatureSet> ranges)
        throws Exception {
        File parent = file.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        try (
            OutputStream fos = Files.newOutputStream(file.toPath());
            OutputStreamWriter osw = new OutputStreamWriter(
                fos,
                StandardCharsets.UTF_8
            )
        ) {
            XMLStreamWriter rangesOut =
                XMLOutputFactory.newInstance().createXMLStreamWriter(osw);
            rangesOut.writeStartDocument(
                StandardCharsets.UTF_8.name(),
                XML_VERSION
            );
            rangesOut.writeCharacters(NEW_LINE);
            rangesOut.writeStartElement(LiftVocabulary.LIFT_RANGES_ROOT);
            rangesOut.writeCharacters(NEW_LINE);
            for (FeatureSet r : ranges) {
                writeHeaderRange(rangesOut, r, false);
                rangesOut.writeCharacters(NEW_LINE);
            }
            rangesOut.writeEndElement();
            rangesOut.writeEndDocument();
            rangesOut.flush();
        }
    }

    private void writeHeaderFieldDescription(LiftFieldAndTraitDefinition f)
        throws Exception {
        out.writeStartElement(LiftVocabulary.fieldDefinitionElement(version));
        out.writeAttribute(
            LiftVocabulary.fieldDefinitionNameAttribute(version),
            f.getName()
        );
        if (f.getTargets().size() > 0) {
            out.writeAttribute(
                LiftVocabulary.CLASS_ATTRIBUTE,
                f.getTargetAsString()
            );
        }
        if (f.getDeclaredTypeStr().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.TYPE_ATTRIBUTE,
                f.getDeclaredTypeStr().get()
            );
        }
        if (f.getResolvedFeatureSet().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.OPTION_RANGE_ATTRIBUTE,
                f.getResolvedFeatureSet().get().getId()
            );
        }
        if (f.getWritingSystem().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE,
                f.getWritingSystem().get()
            );
        }

        if (version == LiftVersion.V0_13) {
            // 0.13 field-defn-content is plain multitext-content: no wrapper elements.
            MultiTextWriters.writeMultiText(out, f.getDescription());
        } else {
            writeMultiTextElement(
                out,
                LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME,
                f.getDescription()
            );
            writeMultiTextElement(
                out,
                LiftVocabulary.LABEL_LOCAL_NAME,
                f.getLabel()
            );
        }

        out.writeEndElement();
    }

    private void writeEntry(LiftEntry entry) throws Exception {
        out.writeStartElement(LiftVocabulary.ENTRY_LOCAL_NAME);
        if (entry.getDateDeleted().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.DATE_DELETED_ATTRIBUTE,
                entry.getDateDeleted().get()
            );
        }
        if (entry.getOrder().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.ORDER_ATTRIBUTE,
                entry.getOrder().get()
            );
        }
        AbstractPropertyWriters.writeAbstractIdentifiable(out, entry);
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, entry);
        AbstractPropertyWriters.writeAbstractNotable(out, entry, version);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, entry, version);

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
        out.writeCharacters(NEW_LINE);
    }

    private void writePronunciation(LiftPronunciation p) throws Exception {
        out.writeStartElement(LiftVocabulary.PRONUNCIATION_LOCAL_NAME);
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, p);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, p, version);
        MultiTextWriters.writeMultiText(out, p.getPronunciation());
        p.getMedias().forEach(unchecked(this::writeMedia));
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeMedia(LiftMedia m) throws Exception {
        out.writeStartElement(LiftVocabulary.MEDIA_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, m.getHref());
        writeUrlRefLabel(m.getLabel());
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    /**
     * Write the optional {@code <label>} of a {@code URLRef-content} element
     * ({@code <media>}, {@code <illustration>}).
     *
     * The forms must be wrapped in {@code <label>}: written as bare children they
     * are invalid, and on re-read the parser attaches them to the enclosing sense
     * instead of to the media or illustration.
     */
    private void writeUrlRefLabel(MultiText label) throws Exception {
        if (label == null || label.isEmpty()) return;
        out.writeStartElement(LiftVocabulary.LABEL_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, label);
        out.writeEndElement();
    }

    private void writeVariant(LiftVariant v) throws Exception {
        out.writeStartElement(LiftVocabulary.VARIANT_LOCAL_NAME);
        refAttribute(v.getRefObject(), v.getRefId().orElse(null))
            .ifPresent(unchecked(ref ->
                out.writeAttribute(LiftVocabulary.REF_ATTRIBUTE, ref)
            ));
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, v);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, v, version);
        v.getPronunciations().forEach(unchecked(this::writePronunciation));
        v.getRelations().forEach(unchecked(this::writeRelation));
        MultiTextWriters.writeMultiText(out, v.getForms());
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeRelation(LiftRelation r) throws Exception {
        out.writeStartElement(LiftVocabulary.RELATION_LOCAL_NAME);
        out.writeAttribute(
            LiftVocabulary.TYPE_ATTRIBUTE,
            r.getType().getId()
        );
        // relation-content makes @ref required: write it even when the target is
        // unknown, rather than emitting a relation the schema (and this reader) reject.
        out.writeAttribute(
            LiftVocabulary.REF_ATTRIBUTE,
            refAttribute(r.getRefObject(), r.getRefId().orElse(null)).orElse("")
        );
        if (r.getOrder().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.ORDER_ATTRIBUTE,
                r.getOrder().get().toString()
            );
        }
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, r);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, r, version);
        if (!r.getUsage().isEmpty()) {
            out.writeStartElement(LiftVocabulary.USAGE_LOCAL_NAME);
            MultiTextWriters.writeMultiText(out, r.getUsage());
            out.writeEndElement();
        }
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeReversal(LiftReversal rev) throws Exception {
        out.writeStartElement(LiftVocabulary.REVERSAL_LOCAL_NAME);
        if (rev.getType() != null) {
            out.writeAttribute(
                LiftVocabulary.TYPE_ATTRIBUTE,
                rev.getType().getId()
            );
        }
        MultiTextWriters.writeMultiText(out, rev.getForms());
        writeReversalMain(rev.getMain());
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    /**
     * Write the {@code <main>} of a reversal.
     *
     * {@code reversal-main} nests {@code <main>} inside {@code <main>}; writing a
     * whole {@code <reversal>} element there instead produced a document this very
     * package could not read back.
     */
    private void writeReversalMain(LiftReversal main) throws Exception {
        if (main == null) return;
        out.writeStartElement(LiftVocabulary.MAIN_LOCAL_NAME);
        MultiTextWriters.writeMultiText(out, main.getForms());
        writeReversalMain(main.getMain());
        out.writeEndElement();
    }

    private void writeEtymology(LiftEtymology e) throws Exception {
        out.writeStartElement(LiftVocabulary.ETYMOLOGY_LOCAL_NAME);
        if (e.getType() != null) {
            out.writeAttribute(
                LiftVocabulary.TYPE_ATTRIBUTE,
                e.getType().getId()
            );
        }
        // etymology-content makes @source required, so write it even when empty:
        // omitting it produces a document this package cannot read back.
        out.writeAttribute(
            LiftVocabulary.SOURCE_ATTRIBUTE,
            e.getSource() == null ? "" : e.getSource()
        );
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, e);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, e, version);

        MultiTextWriters.writeMultiText(out, e.getForms());
        MultiTextWriters.writeMultiText(
            out,
            LiftVocabulary.GLOSS_LOCAL_NAME,
            e.getGlosses()
        );

        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeSense(LiftSense sense) throws Exception {
        writeSense(sense, LiftVocabulary.SENSE_LOCAL_NAME);
    }

    /**
     * @param elementName {@code sense} at the top level, {@code subsense} when nested:
     *        {@code sense-content} has no nested {@code <sense>}.
     */
    private void writeSense(LiftSense sense, String elementName) throws Exception {
        out.writeStartElement(elementName);
        if (sense.getOrder().isPresent()) {
            out.writeAttribute(
                LiftVocabulary.ORDER_ATTRIBUTE,
                sense.getOrder().get().toString()
            );
        }
        AbstractPropertyWriters.writeAbstractIdentifiable(out, sense);
        AbstractPropertyWriters.writeAbstractExtensibleWithoutField(out, sense);
        AbstractPropertyWriters.writeAbstractNotable(out, sense, version);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, sense, version);

        MultiTextWriters.writeMultiText(
            out,
            LiftVocabulary.GLOSS_LOCAL_NAME,
            sense.getGlosses()
        );

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
        sense.getSenses().forEach(
            unchecked(sub -> writeSense(sub, LiftVocabulary.SUBSENSE_LOCAL_NAME))
        );

        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeGrammaticalInfo(GrammaticalInfo gi) throws Exception {
        out.writeStartElement(LiftVocabulary.GRAM_INFO_LOCAL_NAME);
        out.writeAttribute(LiftVocabulary.VALUE_ATTRIBUTE, gi.getGramInfoValue().getId());
        gi.getTraits().forEach(unchecked(this::writeTrait));
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
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
        AbstractPropertyWriters.writeAbstractNotable(out, ex, version);
        AbstractPropertyWriters.writeAbstractExtensibleWithField(out, ex, version);

        MultiTextWriters.writeMultiText(out, ex.getExample());

        ex.getTranslations().forEach(
            biunchecked((type, mt) -> {
                out.writeStartElement(LiftVocabulary.TRANSLATION_LOCAL_NAME);
                out.writeAttribute(LiftVocabulary.TYPE_ATTRIBUTE, type.getId());
                MultiTextWriters.writeMultiText(out, mt);
                out.writeEndElement();
            })
        );
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeIllustration(LiftIllustration il) throws Exception {
        out.writeStartElement(LiftVocabulary.ILLUSTRATION_LOCAL_NAME);
        if (il.getHref() != null) {
            out.writeAttribute(LiftVocabulary.HREF_ATTRIBUTE, il.getHref());
        }
        writeUrlRefLabel(il.getLabel());
        out.writeEndElement();
        out.writeCharacters(NEW_LINE);
    }

    private void writeTrait(LiftTrait t) throws Exception {
        AbstractPropertyWriters.writeTrait(out, t);
        out.writeCharacters(NEW_LINE);
    }

    /**
     * The {@code @ref} of a variant or relation.
     *
     * {@code @ref} is required by the schema, so prefer the resolved target's id and
     * fall back to the raw reference read from the document rather than omitting the
     * attribute. Returns empty only when nothing at all is known.
     */
    private static java.util.Optional<String> refAttribute(
        fr.cnrs.lacito.liftapi.model.AbstractIdentifiable target,
        String rawRefId
    ) {
        if (target != null && target.getId().isPresent()) {
            return target.getId();
        }
        if (rawRefId != null && !rawRefId.isBlank()) {
            return java.util.Optional.of(rawRefId);
        }
        return java.util.Optional.empty();
    }

    // Helper for unchecked exceptions in lambdas
    private static <T> java.util.function.Consumer<T> unchecked(
        ThrowingConsumer<T> f
    ) {
        return t -> {
            try {
                f.accept(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private static <T, U> java.util.function.BiConsumer<T, U> biunchecked(
        ThrowingBiConsumer<T, U> f
    ) {
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
