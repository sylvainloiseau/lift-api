package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementBuilder;
import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementWithFieldAndNoteAndIdBuilder;
import fr.cnrs.lacito.liftapi.builder.AbstractLiftElementWithoutFieldBuilder;
import fr.cnrs.lacito.liftapi.builder.DictionaryComponentBuilderFactory;
import fr.cnrs.lacito.liftapi.builder.EntryBuilder;
import fr.cnrs.lacito.liftapi.builder.EtymologyBuilder;
import fr.cnrs.lacito.liftapi.builder.ExampleBuilder;
import fr.cnrs.lacito.liftapi.builder.FeatureBuilder;
import fr.cnrs.lacito.liftapi.builder.FieldBuilder;
import fr.cnrs.lacito.liftapi.builder.FieldDefinitionBuilder;
import fr.cnrs.lacito.liftapi.builder.NoteBuilder;
import fr.cnrs.lacito.liftapi.builder.RelationBuilder;
import fr.cnrs.lacito.liftapi.builder.ReversalBuilder;
import fr.cnrs.lacito.liftapi.builder.SenseBuilder;
import fr.cnrs.lacito.liftapi.builder.AnnotationBuilder;
import fr.cnrs.lacito.liftapi.builder.VariantBuilder;
import fr.cnrs.lacito.liftapi.internal.BuilderInternals;
import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.MultiText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Turns LIFT XML elements into builder calls.
 *
 * Every component of a loaded dictionary is created by the builders of the
 * {@code builder} package, the same code an application uses: the builders are the one
 * place that knows how a type is resolved, how a value is parsed and how a component is
 * wired. What is left here is what only XML knows:
 *
 * <ul>
 * <li>which builder an element calls for, and what each of its attributes means - the
 * builder method it maps to. An attribute LIFT does not define on that element is
 * logged as a warning, since it is most likely a mistake in the file, and stored in
 * {@code AbstractLiftRoot.getOtherXmlAttributes()}; it is not written back on save;</li>
 * <li>the spellings that differ between LIFT 0.13 and 0.15 ({@code @type} or
 * {@code @name} on a field, {@code <field tag>} in a 0.13 header);</li>
 * <li>what a file may refer to before declaring it: the {@code option-range} of a
 * definition and the {@code parent} of a range element are resolved at the end of the
 * header, the {@code ref} of relations and variants at the end of the document.</li>
 * </ul>
 *
 * It is kept apart from {@link LiftSaxHandler} on purpose: the handler answers "which
 * element is open", this class "what this element and its attributes mean".
 *
 * <h2>Open elements</h2>
 *
 * The handler drives the builders from SAX events rather than from nested lambdas: each
 * open component element is a {@link Frame}, holding the builder created at the start
 * tag, which {@link #close(Frame)} builds at the end tag. Children are therefore built
 * before their parent, into a parent that is not attached yet, so nothing is registered
 * until the {@code <entry>} is built: its {@code build()} registers the whole subtree
 * once, as {@code LiftDictionary.addEntry} does for any detached entry.
 *
 * A frame may also hold a component that already exists and is only being filled: the
 * header, a built-in feature set (the types of notes, of relations...) that the file
 * declares again, a range element declared twice. Those have no builder.
 *
 * <h2>Ranges defined in an external file</h2>
 *
 * A {@code <range href="...">} takes its content from a {@code .lift-ranges} file
 * ({@link ExternalRangesFiles}): {@link LiftSaxHandler} replays the recorded range into the
 * open {@code <range>} as if it were written there ({@link #setReplaying(boolean)}), then
 * reads the {@code <range>}'s own children, which override it: an inline range element
 * replaces the file's one of the same id, an inline label, description or abbreviation of
 * the range replaces the file's, inline traits, fields and annotations of the range are
 * added. What was declared inline is flagged on the set, for the writer. The {@code href}
 * is set when the {@code <range>} closes, once the set is filled, and makes it read-only.
 *
 * A range the file lacks, or a file that is not found, gives an empty range with a
 * warning (FLEx leaves empty ranges out of its ranges file), or an error in strict mode.
 */
final class LiftXmlDecoder {

    private static final Logger LOGGER = Logger.getLogger(LiftXmlDecoder.class.getName());

    private static final BuilderInternals.Access BUILDERS = BuilderInternals.get();

    private static final ModelInternals.Access MODEL = ModelInternals.get();

    /**
     * An open component element: the component, and the builder that builds it at the
     * end tag, or {@code null} if the component already existed.
     */
    record Frame(AbstractLiftRoot element, AbstractLiftElementBuilder<?, ?> builder) {

        static Frame of(AbstractLiftElementBuilder<?, ?> builder) {
            return new Frame(BUILDERS.element(builder), builder);
        }

        static Frame existing(AbstractLiftRoot element) {
            return new Frame(element, null);
        }
    }

    private final LiftDictionary dictionary;
    private final LiftHeader header;
    private final DictionaryComponentBuilderFactory factory;

    /** Feature set id -&gt; parent feature id -&gt; the features declaring that parent. */
    private final Map<String, Map<String, List<Feature>>> childFeaturesByParent = new HashMap<>();

    /** Range id -&gt; the definitions taking their values from it. */
    private final Map<String, List<LiftFieldAndTraitDefinition>> definitionsByOptionRange = new HashMap<>();

    /** Where the parser is, for the warnings; {@code null} if the parser gives none. */
    private Locator locator;

    /** Whether a missing external range is an error rather than a warning. */
    private final boolean strict;

    /** The ranges files the header refers to, read once each, until the end of the header. */
    private final ExternalRangesFiles externalRanges;

    /** The ids of the ranges the file declared, and those declared with an {@code href}. */
    private final Set<String> rangesDeclared = new HashSet<>();
    private final Set<String> rangesDeclaredWithHref = new HashSet<>();

    /** The open {@code <range>}: its set, and its {@code href} and file if it has one. */
    private FeatureSet openRange;
    private String openRangeHref;
    private Path openRangeFile;
    private boolean openRangeMissing;

    /** The recorded range the handler is to replay into the open {@code <range>}, or {@code null}. */
    private RecordedElement rangeToReplay;

    /** Whether the handler is replaying a recorded range (not reading the {@code .lift} file). */
    private boolean replaying;

    /**
     * The range elements declared so far, as opposed to the bare features a value created
     * before its range element was read (a trait of a range element naming a feature of a
     * range declared further down).
     */
    private final Set<Feature> rangeElementsDeclared = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * The names of traits and fields already warned about: their definition is for the
     * other kind of component, so they do not take it (one warning per name, not per
     * trait).
     */
    private final Set<String> namesWarnedAboutTheirDefinitionKind = new HashSet<>();

    /**
     * @param baseDirectory the directory of the {@code .lift} file, against which the
     *        {@code href} of ranges are resolved
     * @param strict whether a missing external range is an error rather than a warning
     */
    LiftXmlDecoder(LiftDictionary dictionary, Path baseDirectory, boolean strict) {
        this.dictionary = dictionary;
        this.header = dictionary.getHeader();
        this.factory = dictionary.getComponentBuilder();
        this.externalRanges = new ExternalRangesFiles(baseDirectory);
        this.strict = strict;
    }

    /** Whether the handler is replaying a recorded range of an external file. */
    void setReplaying(boolean replaying) {
        this.replaying = replaying;
    }

    /**
     * The recorded range of an external file the handler is to replay into the
     * {@code <range>} just opened, or {@code null}; cleared by the call.
     */
    RecordedElement takeRangeToReplay() {
        RecordedElement range = rangeToReplay;
        rangeToReplay = null;
        return range;
    }

    /** The file of the open {@code <range href>}, for the messages of errors raised while it is replayed. */
    Path openRangeFile() {
        return openRangeFile;
    }

    /**
     * Whether the element being read is a child of a {@code <range href>} in the
     * {@code .lift} file itself: an override of the external file, declared inline.
     */
    private boolean readingInlineOverride() {
        return openRangeHref != null && !replaying;
    }

    void setLocator(Locator locator) {
        this.locator = locator;
    }

    // ------------------------------------------------------------------
    // The document
    // ------------------------------------------------------------------

    void startLift(Attributes attributes) {
        String version = attributes.getValue(LiftVocabulary.VERSION_ATTRIBUTE);
        if (version == null) {
            throw new UnsupportedVersionException(
                "The <lift> element has no 'version' attribute; " +
                    "supported versions are 0.13 and 0.15"
            );
        }
        switch (version) {
            case "0.13" -> dictionary.setLiftVersion(LiftVersion.V0_13);
            case "0.15" -> dictionary.setLiftVersion(LiftVersion.V0_15);
            default -> throw new UnsupportedVersionException(
                "Cannot read lift dictionary with version '" +
                    version +
                    "'; supported versions are 0.13 and 0.15"
            );
        }
        dictionary.setLiftProducer(attributes.getValue(LiftVocabulary.PRODUCER_ATTRIBUTE));
    }

    /**
     * The end of the document: every entry and sense has been read, so the references
     * of relations and variants to targets further down the file can be resolved.
     */
    void endDocument() {
        dictionary.resolveReferences();
    }

    /**
     * Build the component of an element that is closing, if it was created by this
     * element (rather than only filled).
     */
    void close(Frame frame) {
        if (frame.builder() != null) {
            frame.builder().build();
        }
    }

    // ------------------------------------------------------------------
    // Entries and their components
    // ------------------------------------------------------------------

    Frame entry(Attributes attributes) {
        EntryBuilder b = factory.entry();
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame sense(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof HasSense hs)) {
            throw new IllegalStateException(
                "Expecting a sense holding object (Entry or Sense), found: " + parent
            );
        }
        SenseBuilder b = factory.sense(hs);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame etymology(AbstractLiftRoot parent, Attributes attributes) {
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type == null) throw new IllegalArgumentException(
            "etymology-content requires a 'type' attribute"
        );
        String source = attribute(attributes, LiftVocabulary.SOURCE_ATTRIBUTE);
        if (source == null) throw new IllegalArgumentException(
            "etymology-content requires a 'source' attribute"
        );
        EtymologyBuilder b = factory.etymology((LiftEntry) parent, type, source);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame relation(AbstractLiftRoot parent, Attributes attributes) {
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type == null) throw new IllegalArgumentException(
            "A relation element must have a type attribute"
        );
        RelationBuilder b = factory.relation(type, (HasRelations) parent);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame variant(AbstractLiftRoot parent, Attributes attributes) {
        VariantBuilder b = factory.variant((LiftEntry) parent);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame example(AbstractLiftRoot parent, Attributes attributes) {
        ExampleBuilder b = factory.example((LiftSense) parent);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame translation(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof LiftExample example)) {
            throw new IllegalStateException("A translation must be in an example, found: " + parent);
        }
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type == null) type = LiftExample.DEFAULT_TRANSLATION_TYPE;
        return rest(Frame.of(factory.translation(example, type)), attributes,
            LiftVocabulary.TYPE_ATTRIBUTE);
    }

    Frame pronunciation(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof HasPronunciation hp)) {
            throw new IllegalStateException("Should implement HasPronunciation; found: " + parent);
        }
        var b = factory.pronunciation(hp);
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame media(AbstractLiftRoot parent, Attributes attributes) {
        return rest(Frame.of(factory.media(
            (LiftPronunciation) parent,
            attribute(attributes, LiftVocabulary.HREF_ATTRIBUTE)
        )), attributes, LiftVocabulary.HREF_ATTRIBUTE);
    }

    Frame illustration(AbstractLiftRoot parent, Attributes attributes) {
        return rest(Frame.of(factory.illustration(
            (LiftSense) parent,
            attribute(attributes, LiftVocabulary.HREF_ATTRIBUTE)
        )), attributes, LiftVocabulary.HREF_ATTRIBUTE);
    }

    Frame note(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof HasNote hn)) {
            throw new IllegalStateException("Expecting an AbstractNotable, found: " + parent);
        }
        NoteBuilder b = factory.note(hn, attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE));
        populate(b, attributes);
        return Frame.of(b);
    }

    Frame field(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof HasField hf)) {
            throw new IllegalStateException("Expecting a field-holding object, found: " + parent);
        }
        // field-content names its definition with @type in LIFT 0.13 and with @name
        // in LIFT 0.15. Accept either, whatever the declared version, so that files
        // mixing the two (FLEx does) still load.
        String name = attribute(attributes, LiftVocabulary.NAME_ATTRIBUTE);
        if (name == null) name = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (name == null) throw new IllegalArgumentException(
            "A field element requires a 'name' (LIFT 0.15) or 'type' (LIFT 0.13) attribute"
        );
        warnIfTheDefinitionIsForTheOtherKind(name, false);
        FieldBuilder b = factory.field(hf, name);
        String created = attribute(attributes, LiftVocabulary.DATE_CREATED_ATTRIBUTE);
        if (created != null) b.dateCreated(created);
        String modified = attribute(attributes, LiftVocabulary.DATE_MODIFIED_ATTRIBUTE);
        if (modified != null) b.dateModified(modified);
        return rest(inlineIfOnTheOpenRange(parent, Frame.of(b)), attributes,
            LiftVocabulary.NAME_ATTRIBUTE, LiftVocabulary.TYPE_ATTRIBUTE,
            LiftVocabulary.DATE_CREATED_ATTRIBUTE, LiftVocabulary.DATE_MODIFIED_ATTRIBUTE);
    }

    Frame trait(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof HasTrait ht)) {
            throw new IllegalStateException("Expecting an objet implementing HasTrait, found: " + parent);
        }
        String name = attribute(attributes, LiftVocabulary.NAME_ATTRIBUTE);
        warnIfTheDefinitionIsForTheOtherKind(name, true);
        return rest(inlineIfOnTheOpenRange(parent, Frame.of(factory.trait(
            ht,
            name,
            attribute(attributes, LiftVocabulary.VALUE_ATTRIBUTE)
        ))), attributes, LiftVocabulary.NAME_ATTRIBUTE, LiftVocabulary.VALUE_ATTRIBUTE);
    }

    /**
     * A trait, field or annotation of the open {@code <range href>} itself, read from the
     * {@code .lift} file: it is added to the external range, and flagged as declared
     * inline so that it is written back.
     */
    private Frame inlineIfOnTheOpenRange(Object parent, Frame frame) {
        if (parent == openRange && readingInlineOverride()) {
            MODEL.markDeclaredInline(openRange, frame.element());
        }
        return frame;
    }

    /**
     * A trait name whose definition is for fields (a {@code multitext}...), or a field
     * name whose definition is for traits, does not take that definition (see
     * {@link LiftHeader#resolveTraitSpecification(String)}): say so, once per name.
     */
    private void warnIfTheDefinitionIsForTheOtherKind(String name, boolean forATrait) {
        if (name == null || !header.containsFieldsAndTraitsDefinitions(name)) {
            return;
        }
        LiftFieldAndTraitDefinition definition = header.getFieldsAndTraitsDefinitions(name);
        boolean fits = forATrait ? definition.fitsTraits() : definition.fitsFields();
        if (!fits && namesWarnedAboutTheirDefinitionKind.add((forATrait ? "trait " : "field ") + name)) {
            LOGGER.warning(() -> "The definition " + name + " is for " + (forATrait ? "fields" : "traits")
                + ": the " + (forATrait ? "traits" : "fields") + " of that name do not take it"
                + (locator == null ? "" : " (line " + locator.getLineNumber() + ")") + ".");
        }
    }

    Frame grammaticalInfo(AbstractLiftRoot parent, Attributes attributes) {
        String value = attribute(attributes, LiftVocabulary.VALUE_ATTRIBUTE);
        if (value == null) throw new IllegalArgumentException("Grammatical info code cannot be null");
        return rest(Frame.of(factory.grammaticalInfo((LiftSense) parent, value)), attributes,
            LiftVocabulary.VALUE_ATTRIBUTE);
    }

    /**
     * @param parent what the annotation is on: a component, a MultiText or a form
     */
    Frame annotation(HasAnnotation parent, Attributes attributes) {
        String name = attribute(attributes, LiftVocabulary.NAME_ATTRIBUTE);
        if (name == null) throw new IllegalArgumentException(
            "Attribute name on annotation element cannot be null"
        );
        AnnotationBuilder b = factory.annotation(parent, name)
            .withValue(attribute(attributes, LiftVocabulary.VALUE_ATTRIBUTE))
            .withWho(attribute(attributes, LiftVocabulary.WHO_ATTRIBUTE))
            .withWhen(attribute(attributes, LiftVocabulary.WHEN_ATTRIBUTE));
        return rest(inlineIfOnTheOpenRange(parent, Frame.of(b)), attributes,
            LiftVocabulary.NAME_ATTRIBUTE, LiftVocabulary.VALUE_ATTRIBUTE,
            LiftVocabulary.WHO_ATTRIBUTE, LiftVocabulary.WHEN_ATTRIBUTE);
    }

    Frame reversal(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof LiftSense sense)) {
            throw new IllegalStateException("Expecting a LiftSense for reversal, found: " + parent);
        }
        ReversalBuilder b = factory.reversal(sense);
        // reversal-content declares @type as optional.
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type != null) b.withType(type);
        return rest(Frame.of(b), attributes, LiftVocabulary.TYPE_ATTRIBUTE);
    }

    Frame reversalMain(AbstractLiftRoot parent, Attributes attributes) {
        if (!(parent instanceof LiftReversal reversal)) {
            throw new IllegalStateException("Expecting a LiftReversal for main, found: " + parent);
        }
        return rest(Frame.of(factory.reversalMain(reversal)), attributes);
    }

    /**
     * A {@code <gloss>} opening in a sense or an etymology: the form is given to the
     * builder now, and filled with its text and spans as they arrive.
     */
    void gloss(Frame frame, Form gloss) {
        switch (frame.builder()) {
            case SenseBuilder b -> b.withGloss(gloss);
            case EtymologyBuilder b -> b.addGloss(gloss);
            case null, default -> throw new IllegalStateException(
                "gloss element is allowed in etymology and sense; found: " + frame.element()
            );
        }
    }

    /**
     * Add a form to the text it belongs to.
     *
     * Texts are filled before their component is built, so they are not registered yet
     * and accept any language: registration declares the languages it finds. The texts
     * of the header components that existed before the file was read (a built-in feature
     * set, declared again in the file) are registered already, and only accept declared
     * languages; the file is the authority on the languages it uses, so the language is
     * declared here first.
     */
    void addForm(MultiText text, Form form) {
        if (text.getUUID() != null) {
            LiftDictionaryLanguagesManager languages = dictionary.getMetaLanguageManager();
            if (!languages.hasLanguage(form.getLang())) {
                languages.addLanguage(form.getLang());
            }
        }
        text.add(form);
    }

    /**
     * The attributes of the components that carry the common ones (ids, dates,
     * references...), applied in document order. An attribute LIFT does not define on
     * the element goes to {@link #other}.
     *
     * The elements whose builder does not take these common attributes read theirs by
     * name, and hand the others to {@link #rest}.
     */
    private void populate(AbstractLiftElementWithoutFieldBuilder<?, ?> b, Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case LiftVocabulary.ID_ATTRIBUTE -> {
                    if (b instanceof AbstractLiftElementWithFieldAndNoteAndIdBuilder<?, ?> ib) ib.withId(value);
                    else other(b, name, value);
                }
                case LiftVocabulary.GUID_ATTRIBUTE -> {
                    if (b instanceof AbstractLiftElementWithFieldAndNoteAndIdBuilder<?, ?> ib) ib.withGuid(value);
                    else other(b, name, value);
                }
                case LiftVocabulary.TYPE_ATTRIBUTE -> {
                    // Given to the builder's constructor.
                    if (!(b instanceof EtymologyBuilder || b instanceof RelationBuilder || b instanceof NoteBuilder)) {
                        other(b, name, value);
                    }
                }
                case LiftVocabulary.SOURCE_ATTRIBUTE -> {
                    if (b instanceof ExampleBuilder eb) eb.withSource(value);
                    else if (!(b instanceof EtymologyBuilder)) other(b, name, value);
                }
                case LiftVocabulary.REF_ATTRIBUTE -> {
                    if (!value.trim().isEmpty()) {
                        switch (b) {
                            case VariantBuilder vb -> vb.withRefId(value);
                            case RelationBuilder rb -> rb.withRefId(value);
                            default -> other(b, name, value);
                        }
                    }
                }
                case LiftVocabulary.DATE_DELETED_ATTRIBUTE -> {
                    if (b instanceof EntryBuilder eb) eb.dateDeleted(value);
                    else other(b, name, value);
                }
                case LiftVocabulary.ORDER_ATTRIBUTE -> {
                    switch (b) {
                        case EntryBuilder eb -> eb.withOrder(value);
                        case SenseBuilder sb -> sb.withOrder(Integer.parseInt(value));
                        case RelationBuilder rb -> rb.withOrder(Integer.parseInt(value));
                        default -> other(b, name, value);
                    }
                }
                case LiftVocabulary.DATE_CREATED_ATTRIBUTE -> b.dateCreated(value);
                case LiftVocabulary.DATE_MODIFIED_ATTRIBUTE -> b.dateModified(value);
                default -> other(b, name, value);
            }
        }
    }

    /**
     * An attribute LIFT does not define on this element. A file written by a LIFT tool
     * has none (the reference corpus has none), so it is reported: most likely a
     * misspelling, or an attribute this reader should map and does not. It is kept in
     * {@code getOtherXmlAttributes()} for the application to inspect, but the writer
     * does not write it back.
     */
    private void other(AbstractLiftElementBuilder<?, ?> b, String name, String value) {
        other(BUILDERS.element(b), name, value);
    }

    private void other(AbstractLiftRoot element, String name, String value) {
        LOGGER.warning(() -> "Attribute " + name + "=\"" + value + "\" is not defined on "
            + element.getClass().getSimpleName()
            + (locator == null ? "" : " (line " + locator.getLineNumber() + ")")
            + "; it will not be saved");
        MODEL.putOtherXmlAttribute(element, name, value);
    }

    /**
     * For an element that reads its attributes by name rather than through
     * {@link #populate}: hand every attribute it did not read to {@link #other}, so that
     * an unexpected one is reported rather than silently ignored.
     *
     * @param frame the element, built or merely filled
     * @param known the attributes the element has read
     * @return {@code frame}, for chaining
     */
    private Frame rest(Frame frame, Attributes attributes, String... known) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            if (!List.of(known).contains(name)) {
                other(frame.element(), name, attributes.getValue(i));
            }
        }
        return frame;
    }

    private static String attribute(Attributes attributes, String name) {
        return attributes.getValue(LiftVocabulary.LIFT_URI, name);
    }

    // ------------------------------------------------------------------
    // The header
    // ------------------------------------------------------------------

    Frame header() {
        // The traits of the ranges and range elements are read before the definitions,
        // whose option-range is set only by endHeader(): they are rebound then.
        ModelInternals.get().suspendRebinding(header);
        return Frame.existing(header);
    }

    /**
     * A {@code <range>}: a new feature set, or one the header already has - a built-in
     * one (the types of notes, relations...) that the file declares, which is filled in
     * place.
     *
     * With an {@code href}, the range of that id in the external file is looked up, and
     * left for the handler to replay ({@link #takeRangeToReplay()}); its attributes (guid,
     * dates) are applied to the set, then those of the {@code .lift} file's element. The
     * {@code href} itself is set by {@link #closeRange(Frame)}.
     *
     * A range declared twice is merged into the first declaration, unless one of them has
     * an {@code href}: which one would win is unclear, so that is an error.
     */
    Frame range(Attributes attributes) {
        String id = attribute(attributes, LiftVocabulary.ID_ATTRIBUTE);
        if (id == null) throw new IllegalArgumentException("Range ID cannot be null");
        String href = attribute(attributes, LiftVocabulary.HREF_ATTRIBUTE);
        if (rangesDeclared.contains(id) && (href != null || rangesDeclaredWithHref.contains(id))) {
            throw new IllegalArgumentException(
                "The range " + id + " is declared twice, and one of the declarations refers to an external file.");
        }
        rangesDeclared.add(id);
        if (href != null) {
            rangesDeclaredWithHref.add(id);
        }
        Frame frame;
        if (header.hasFeatureSet(id)) {
            // Filled in place: it has no builder.
            frame = Frame.existing(header.getFeatureSet(id));
        } else {
            frame = Frame.of(factory.range(id));
        }
        FeatureSet set = (FeatureSet) frame.element();
        openRange = set;
        if (href != null) {
            ExternalRangesFiles.Lookup lookup = externalRanges.find(href, id);
            openRangeHref = href;
            openRangeFile = lookup.file();
            openRangeMissing = lookup.range() == null;
            if (lookup.range() == null) {
                if (strict) {
                    throw new IllegalArgumentException("The range " + id + " cannot be read: " + lookup.problem() + ".");
                }
                LOGGER.warning(() -> "The range " + id + " is empty: " + lookup.problem()
                    + (locator == null ? "" : " (line " + locator.getLineNumber() + ")") + ".");
            } else {
                rangeToReplay = lookup.range();
                applyRangeAttributes(set, lookup.range().attributes());
            }
        }
        applyRangeAttributes(set, attributes);
        return frame;
    }

    /** The attributes of a {@code <range>} other than its id and href. */
    private void applyRangeAttributes(FeatureSet set, Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case LiftVocabulary.ID_ATTRIBUTE, LiftVocabulary.HREF_ATTRIBUTE -> { }
                case LiftVocabulary.GUID_ATTRIBUTE -> set.setGuid(value);
                case LiftVocabulary.DATE_CREATED_ATTRIBUTE -> set.setDateCreated(value);
                case LiftVocabulary.DATE_MODIFIED_ATTRIBUTE -> set.setDateModified(value);
                default -> other(set, name, value);
            }
        }
    }

    /**
     * The end of a {@code <range>}: the set is built (added to the header) if it is new,
     * and, if it refers to an external file, gets its {@code href} - which makes it
     * read-only - now that it holds the file's content and the inline overrides.
     */
    void closeRange(Frame frame) {
        close(frame);
        if (openRangeHref != null) {
            MODEL.setHref(openRange, openRangeHref);
            MODEL.setExternalDefinition(openRange, openRangeFile, openRangeMissing);
        }
        openRange = null;
        openRangeHref = null;
        openRangeFile = null;
        openRangeMissing = false;
    }

    /**
     * A {@code <label>}, {@code <description>} or {@code <abbrev>} of the open
     * {@code <range href>} itself, read from the {@code .lift} file: it replaces the
     * external file's, and is flagged as declared inline.
     */
    void rangeText(FeatureSet set, MultiText text) {
        if (set != openRange || !readingInlineOverride()) {
            return;
        }
        for (Form form : List.copyOf(text.getForms())) {
            text.removeForm(form.getLang());
        }
        MODEL.markDeclaredInline(set, text);
    }

    /**
     * A {@code <range-element>} of the open feature set. Its {@code parent} may be
     * declared further down the header, so it is resolved by {@link #endHeader()}.
     *
     * A range element declared twice in a range keeps its first declaration; the others
     * are skipped, with a warning (FLEx cuts ids at the first space, so that
     * {@code "non -dual"} and {@code "non -plural"} are both {@code non}). In the
     * {@code <range href>} of the {@code .lift} file, an element overrides the external
     * file's element of that id instead.
     *
     * @return the frame, or {@code null} if the element is to be skipped
     */
    Frame rangeElement(AbstractLiftRoot parent, Attributes attributes) {
        FeatureSet set = (FeatureSet) parent;
        String id = attribute(attributes, LiftVocabulary.ID_ATTRIBUTE);
        if (id == null) throw new IllegalArgumentException("range-element requires an 'id' attribute");
        String guid = attribute(attributes, LiftVocabulary.GUID_ATTRIBUTE);
        String created = attribute(attributes, LiftVocabulary.DATE_CREATED_ATTRIBUTE);
        String modified = attribute(attributes, LiftVocabulary.DATE_MODIFIED_ATTRIBUTE);

        boolean declaredAlready = set.hasFeature(id) && rangeElementsDeclared.contains(set.getFeature(id));
        if (declaredAlready && !(readingInlineOverride() && !set.getFeature(id).isDeclaredInline())) {
            LOGGER.warning(() -> "The range element " + id + " of the range " + set.getId()
                + " is declared twice; the second declaration is ignored"
                + (locator == null ? "" : " (line " + locator.getLineNumber() + ")") + ".");
            return null;
        }
        if (declaredAlready) {
            // An inline element overrides the external file's element of that id, as a
            // whole: the file's one goes. Nothing uses it yet, but a child may have named
            // it as its parent: that is resolved by id at the end of the header.
            Feature overridden = set.removeFeature(id);
            Map<String, List<Feature>> children = childFeaturesByParent.get(set.getId());
            if (children != null) {
                children.values().forEach(list -> list.remove(overridden));
            }
        }
        Frame frame;
        if (set.hasFeature(id)) {
            // A bare feature a value created before its declaration: filled in place.
            Feature existing = set.getFeature(id);
            if (guid != null) existing.setGuid(guid);
            if (created != null) existing.setDateCreated(created);
            if (modified != null) existing.setDateModified(modified);
            frame = Frame.existing(existing);
        } else {
            FeatureBuilder b = factory.feature(set, id);
            if (guid != null) b.withGuid(guid);
            if (created != null) b.dateCreated(created);
            if (modified != null) b.dateModified(modified);
            frame = Frame.of(b);
        }

        rangeElementsDeclared.add((Feature) frame.element());
        if (readingInlineOverride()) {
            MODEL.markDeclaredInline(set, frame.element());
        }
        String parentId = attribute(attributes, LiftVocabulary.PARENT_ATTRIBUTE);
        if (parentId != null) {
            childFeaturesByParent
                .computeIfAbsent(set.getId(), k -> new HashMap<>())
                .computeIfAbsent(parentId, k -> new ArrayList<>())
                .add((Feature) frame.element());
        }
        return rest(frame, attributes,
            LiftVocabulary.ID_ATTRIBUTE, LiftVocabulary.GUID_ATTRIBUTE, LiftVocabulary.PARENT_ATTRIBUTE,
            LiftVocabulary.DATE_CREATED_ATTRIBUTE, LiftVocabulary.DATE_MODIFIED_ATTRIBUTE);
    }

    /**
     * A {@code <field-definition>}, or a {@code <field tag="...">} in a LIFT 0.13 header.
     * Its {@code option-range} may be declared further down the header, so it is resolved
     * by {@link #endHeader()}.
     */
    Frame fieldDefinition(Attributes attributes) {
        String name = attribute(attributes, LiftVocabulary.NAME_ATTRIBUTE);
        if (name == null) name = attribute(attributes, LiftVocabulary.TAG_ATTRIBUTE);
        if (name == null) name = attribute(attributes, LiftVocabulary.GUID_ATTRIBUTE);
        if (name == null) throw new IllegalArgumentException(
            "An attribute 'name', 'tag', or 'guid' is required on field-definition"
        );
        if (header.containsFieldsAndTraitsDefinitions(name)) {
            // Files exported by FieldWorks may declare a definition twice: the last
            // declaration wins, as it always did.
            LOGGER.warning("Field or trait definition declared twice, the last one is kept: " + name);
            LiftFieldAndTraitDefinition existing = header.getFieldsAndTraitsDefinitions(name);
            // The definitions met here are all declarations: a trait or a field of an
            // undeclared name creates none. The only components that could use one by
            // now are those held by the header, and they are bound to the definitions
            // only at endHeader(): this branch is kept for a header built otherwise.
            if (header.isUsed(existing)) {
                return redeclared(existing, attributes);
            }
            header.removeFieldOrTraitDefinition(existing);
        }
        FieldDefinitionBuilder b = factory.fieldDefinition(name);

        String targets = attribute(attributes, LiftVocabulary.CLASS_ATTRIBUTE);
        if (targets != null) b.withTargets(targets);
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type != null) b.withDataModel(type);
        String writingSystem = attribute(attributes, LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE);
        if (writingSystem != null) b.withWritingSystem(writingSystem);

        Frame frame = Frame.of(b);
        String optionRange = attribute(attributes, LiftVocabulary.OPTION_RANGE_ATTRIBUTE);
        if (optionRange != null) {
            definitionsByOptionRange
                .computeIfAbsent(optionRange, k -> new ArrayList<>())
                .add((LiftFieldAndTraitDefinition) frame.element());
        }
        return rest(frame, attributes,
            LiftVocabulary.NAME_ATTRIBUTE, LiftVocabulary.TAG_ATTRIBUTE, LiftVocabulary.GUID_ATTRIBUTE,
            LiftVocabulary.CLASS_ATTRIBUTE, LiftVocabulary.TYPE_ATTRIBUTE,
            LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE, LiftVocabulary.OPTION_RANGE_ATTRIBUTE);
    }

    /**
     * A definition declared again while fields or traits read earlier already hold it:
     * the traits of a range element, read before the {@code <fields>} of the header,
     * create their definition on the fly. It cannot be replaced under them, so the
     * declaration is applied to it in place - the attributes it gives, and its label and
     * description instead of the earlier ones - which is what "the last declaration wins"
     * means for a definition in use.
     */
    private Frame redeclared(LiftFieldAndTraitDefinition existing, Attributes attributes) {
        String targets = attribute(attributes, LiftVocabulary.CLASS_ATTRIBUTE);
        if (targets != null) existing.setTargets(targets);
        String type = attribute(attributes, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type != null) existing.setDataModel(Optional.of(type));
        String writingSystem = attribute(attributes, LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE);
        if (writingSystem != null) existing.setWritingSystem(Optional.of(writingSystem));
        for (MultiText text : List.of(existing.getLabel(), existing.getDescription())) {
            for (Form form : List.copyOf(text.getForms())) {
                text.removeForm(form.getLang());
            }
        }
        String optionRange = attribute(attributes, LiftVocabulary.OPTION_RANGE_ATTRIBUTE);
        if (optionRange != null) {
            definitionsByOptionRange
                .computeIfAbsent(optionRange, k -> new ArrayList<>())
                .add(existing);
        }
        return rest(Frame.existing(existing), attributes,
            LiftVocabulary.NAME_ATTRIBUTE, LiftVocabulary.TAG_ATTRIBUTE, LiftVocabulary.GUID_ATTRIBUTE,
            LiftVocabulary.CLASS_ATTRIBUTE, LiftVocabulary.TYPE_ATTRIBUTE,
            LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE, LiftVocabulary.OPTION_RANGE_ATTRIBUTE);
    }

    /**
     * The end of the header: every range and range element is declared, so the
     * {@code option-range} of the definitions and the {@code parent} of the range
     * elements can be resolved.
     */
    void endHeader() {
        // The feature sets of external files are read-only by now; their hierarchy, the
        // bare features a parent names, and the rebinding of their traits are part of
        // reading them.
        MODEL.withoutExternalDefinitionGuard(this::resolveTheHeader);
        externalRanges.clear();
        rangeElementsDeclared.clear();
        rangesDeclared.clear();
        rangesDeclaredWithHref.clear();
    }

    private void resolveTheHeader() {
        for (Map.Entry<String, List<LiftFieldAndTraitDefinition>> e : definitionsByOptionRange.entrySet()) {
            FeatureSet range = header.getFeatureSet(e.getKey());
            for (LiftFieldAndTraitDefinition definition : e.getValue()) {
                definition.setResolvedRange(Optional.of(range));
            }
        }
        definitionsByOptionRange.clear();

        for (Map.Entry<String, Map<String, List<Feature>>> set : childFeaturesByParent.entrySet()) {
            FeatureSet range = header.getFeatureSet(set.getKey());
            for (Map.Entry<String, List<Feature>> byParent : set.getValue().entrySet()) {
                Feature parent = MODEL.getOrCreateUndeclaredFeature(range, byParent.getKey());
                for (Feature child : byParent.getValue()) {
                    child.setSuperordinateFeature(parent);
                }
            }
        }
        childFeaturesByParent.clear();

        ModelInternals.get().resumeRebinding(header);
    }
}
