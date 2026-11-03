package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LoadDiagnostic;
import fr.cnrs.lacito.liftapi.LoadOptions;
import fr.cnrs.lacito.liftapi.NotALiftDocumentException;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.DuplicateIdException;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasGrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.xml.LiftXmlDecoder.Frame;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

/**
 * SAX handler turning a LIFT XML file into a dictionary.
 *
 * It knows the structure of the document: which element is open, the stacks of open
 * components, MultiTexts and forms, and the text and spans of a form. What an element
 * and its attributes mean, and the builder calls they translate into, is the job of
 * {@link LiftXmlDecoder}: every component is created by the builders.
 *
 * A {@code <range href="...">} of the header is filled from the external ranges file it
 * refers to: once the range is open, the range of that id recorded from the file
 * ({@link LiftRangesFileReader}) is replayed through {@link #startElement},
 * {@link #characters} and {@link #endElement}, as if it were written there; errors then
 * name the ranges file and its line. The {@code <range>}'s own children follow, and
 * override it.
 *
 * <h2>What is not LIFT</h2>
 *
 * The root element must be {@code <lift>} in no namespace: anything else is refused at the
 * first start tag ({@code NotALiftDocumentException}), before anything is built. Inside
 * the document, an element that is not LIFT - another name, an element in a namespace
 * (LIFT elements are in none, whatever their local name), a {@code <lift>} other than the
 * root - is skipped with its whole subtree and reported, once per name with a count, as a
 * {@code LoadDiagnostic} ({@link LiftXmlDecoder#diagnose}); a strict load fails on it
 * instead. Nothing is only logged. The checks cost one boolean and one
 * {@code uri.isEmpty()} per element.
 */
public final class LiftSaxHandler extends DefaultHandler {

    private static final boolean DEBUGGING = false;

    private static final Logger LOGGER = Logger.getLogger(
        LiftSaxHandler.class.getName()
    );

    /** Nesting depth of {@code <text>} elements (0 when outside any). */
    private int inTextDepth = 0;

    /**
     * Depth of the subtree currently being skipped because its root element is not
     * part of the LIFT vocabulary. 0 when not skipping.
     */
    private int skipDepth = 0;

    /** Whether the root element has been met (and accepted: it is {@code <lift>}). */
    private boolean rootSeen;

    /** Whether the {@code <lift>} root has been opened: any {@code <lift>} after it is not LIFT. */
    private boolean liftStarted;

    private final LiftXmlDecoder decoder;

    private final boolean strict;

    /** Where the parser is, for the messages of errors raised while building. */
    private Locator locator;

    /**
     * The open component elements, innermost first: the component and the builder that
     * builds it when the element closes (see {@link LiftXmlDecoder.Frame}).
     */
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Deque<MultiText> multiTextStack = new ArrayDeque<>();

    /**
     * The open {@code <form>} / {@code <gloss>} elements, innermost first.
     *
     * This is a stack rather than a single field because the schema allows an
     * {@code <annotation>} - itself a multitext - inside a {@code <form>}: closing the
     * inner form must restore the outer one, not clear it.
     */
    private final Deque<Form> formStack = new ArrayDeque<>();

    private StringBuffer sb;

    /** The ranges file being replayed, or {@code null} while the {@code .lift} file is read. */
    private Path replayedFile;

    /**
     * @param dictionary the dictionary the components read are added to
     * @param options when strict, recoverable parse errors abort the parse instead of
     *        being reported, and so do an external range that cannot be found and what
     *        would be lost on save (an unknown element or attribute...)
     * @param source the file read, against whose directory the {@code href} of ranges
     *        are resolved
     */
    public LiftSaxHandler(LiftDictionary dictionary, LoadOptions options, File source) {
        Path file = source.getAbsoluteFile().toPath();
        this.decoder = new LiftXmlDecoder(dictionary, file, options);
        this.strict = options.strict();
    }

    /** The {@code <form>} or {@code <gloss>} currently open, or {@code null}. */
    private Form currentForm() {
        return formStack.peek();
    }

    /** The component of the innermost open component element. */
    private AbstractLiftRoot top() {
        Frame f = frames.peek();
        return f == null ? null : f.element();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        decoder.setLocator(locator);
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        if (skipDepth > 0) {
            super.characters(ch, start, length);
            return;
        }
        if (inTextDepth > 0) {
            // we are in text
            if (sb == null) sb = new StringBuffer();
            sb.append(ch, start, length);
        }
        super.characters(ch, start, length);
    }

    /**
     * {@link DefaultHandler} discards recoverable parse problems. Report them: a
     * silently dropped error is a silently corrupted dictionary.
     */
    @Override
    public void warning(SAXParseException e) throws SAXException {
        LOGGER.warning(describe("Warning", e));
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        String message = describe("Error", e);
        if (strict) throw new SAXException(message, e);
        decoder.diagnose(LoadDiagnostic.Kind.XML_ERROR, null, message);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        throw new SAXException(describe("Fatal error", e), e);
    }

    private static String describe(String kind, SAXParseException e) {
        return kind +
            " while parsing LIFT at line " +
            e.getLineNumber() +
            ", column " +
            e.getColumnNumber() +
            ": " +
            e.getMessage();
    }

    /**
     * Give an error raised while creating or building a component the position of the
     * element in the file.
     *
     * The builders know nothing of the file, so their exceptions say what is wrong but
     * not where. A {@link DuplicateIdException} keeps its type (callers catch it), and
     * so does an {@link UnsupportedVersionException}; anything else becomes a
     * {@link SAXParseException} carrying the position, with the original as its cause.
     */
    private RuntimeException located(String localName, RuntimeException e) throws SAXException {
        if (locator == null || e instanceof UnsupportedVersionException) {
            return e;
        }
        String where = "<" + localName + "> at line " + locator.getLineNumber() +
            ", column " + locator.getColumnNumber() +
            (replayedFile == null ? "" : " of the ranges file " + replayedFile);
        if (e instanceof DuplicateIdException) {
            DuplicateIdException d = new DuplicateIdException(e.getMessage() + " (" + where + ")");
            d.initCause(e);
            return d;
        }
        throw new SAXParseException("Error while reading " + where + ": " + e.getMessage(), locator, e);
    }

    /**
     * Report an element that is not LIFT, and skip its whole subtree. The diagnostic is
     * counted at the root of the subtree only, not for what it holds.
     *
     * Aborting the parse instead (unless the load is strict) would make any file with a
     * single unexpected element unreadable.
     *
     * @param uri the element's namespace, empty for none
     * @param why what is wrong with it, after "The element <x> (line n)"
     */
    private void skipUnknownElement(String uri, String localName, String why) {
        skip(LoadDiagnostic.Kind.UNKNOWN_ELEMENT, uri.isEmpty() ? localName : "{" + uri + "}" + localName,
            "The element <" + localName + ">" + where() + " " + why
            + ": it is skipped with its content, which will not be saved.");
    }

    /** Report an element and skip its whole subtree. */
    private void skip(LoadDiagnostic.Kind kind, String subject, String message) {
        decoder.diagnose(kind, subject, message);
        skipDepth = 1;
    }

    /** " (line n)", or " (line n of the ranges file f)", or nothing if the parser gives no position. */
    private String where() {
        if (locator == null) return "";
        return " (line " + locator.getLineNumber()
            + (replayedFile == null ? "" : " of the ranges file " + replayedFile.getFileName()) + ")";
    }

    /**
     * The root element must be {@code <lift>} in no namespace: anything else is refused at
     * once, before anything is read. A ranges file opened instead of the {@code .lift} file
     * is told apart.
     */
    private void checkRoot(String uri, String localName) throws SAXException {
        String message;
        if (uri.isEmpty() && LiftVocabulary.LIFT_LOCAL_NAME.equals(localName)) {
            return;
        } else if (uri.isEmpty() && LiftVocabulary.LIFT_RANGES_ROOT.equals(localName)) {
            message = "This is a ranges file (root element <lift-ranges>), not a LIFT dictionary:"
                + " open the .lift file that refers to it";
        } else if (LiftVocabulary.LIFT_LOCAL_NAME.equals(localName)) {
            message = "The root element <lift> is in the namespace " + uri
                + "; LIFT elements are in no namespace: this is not a LIFT document";
        } else {
            message = "The root element is <" + localName + ">"
                + (uri.isEmpty() ? "" : " (namespace " + uri + ")")
                + ", not <lift>: this is not a LIFT document";
        }
        throw new SAXException(new NotALiftDocumentException(message + where() + "."));
    }

    @Override
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes
    ) throws SAXException {
        if (!rootSeen) {
            checkRoot(uri, localName);
            rootSeen = true;
        }
        if (skipDepth > 0) {
            skipDepth++;
            super.startElement(uri, localName, qName, attributes);
            return;
        }
        if (DEBUGGING) LOGGER.info(
            "start element: " +
                localName +
                "; frame stack size: " +
                frames.size() +
                "; Text stack size: " +
                multiTextStack.size()
        );
        try {
            decoder.setCurrentElement(localName);
            if (!uri.isEmpty()) {
                // LIFT elements are in no namespace, whatever their local name.
                skipUnknownElement(uri, localName, "is in the namespace " + uri
                    + " (LIFT elements are in none)");
            } else if (open(localName, attributes)) {
                pushMultiText(localName);
            }
        } catch (RuntimeException e) {
            throw located(localName, e);
        }
        super.startElement(uri, localName, qName, attributes);
        RecordedElement externalRange = decoder.takeRangeToReplay();
        if (externalRange != null) {
            replay(externalRange, decoder.openRangeFile());
        }
    }

    /**
     * Replays the content of a range recorded from an external ranges file into the
     * {@code <range>} just opened, with the locator pointing into that file.
     */
    private void replay(RecordedElement range, Path file) throws SAXException {
        Locator main = locator;
        LocatorImpl inTheRangesFile = new LocatorImpl();
        inTheRangesFile.setSystemId(file.toUri().toString());
        locator = inTheRangesFile;
        decoder.setLocator(inTheRangesFile);
        replayedFile = file;
        decoder.setReplaying(true);
        try {
            for (Object child : range.children()) {
                replay(child, inTheRangesFile);
            }
        } finally {
            decoder.setReplaying(false);
            replayedFile = null;
            locator = main;
            decoder.setLocator(main);
        }
    }

    private void replay(Object node, LocatorImpl inTheRangesFile) throws SAXException {
        if (node instanceof String text) {
            characters(text.toCharArray(), 0, text.length());
            return;
        }
        RecordedElement element = (RecordedElement) node;
        inTheRangesFile.setLineNumber(element.lineNumber());
        inTheRangesFile.setColumnNumber(element.columnNumber());
        startElement(LiftVocabulary.LIFT_URI, element.localName(), element.localName(), element.attributes());
        for (Object child : element.children()) {
            replay(child, inTheRangesFile);
        }
        endElement(LiftVocabulary.LIFT_URI, element.localName(), element.localName());
    }

    /**
     * First step of a start tag: open the component the element creates, if any, or
     * deal with the text model.
     *
     * @return {@code false} if the element is skipped
     */
    private boolean open(String localName, Attributes attributes) {
        switch (localName) {
            case LiftVocabulary.LIFT_LOCAL_NAME:
                if (liftStarted) {
                    // Only the root may be <lift>: one inside would reset the version.
                    skipUnknownElement("", localName, "is inside the document: only the root may be <lift>");
                    return false;
                }
                liftStarted = true;
                decoder.startLift(attributes);
                break;
            case LiftVocabulary.ENTRY_LOCAL_NAME:
                frames.push(decoder.entry(attributes));
                break;
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
                frames.push(decoder.sense(top(), attributes));
                break;
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
                frames.push(decoder.etymology(top(), attributes));
                break;
            case LiftVocabulary.RELATION_LOCAL_NAME: // only entry, sense, variant may have relation (and implement HasRelations)
                frames.push(decoder.relation(top(), attributes));
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                frames.push(decoder.fieldDefinition(attributes));
                break;
            case LiftVocabulary.FIELD_LOCAL_NAME:
                if (top() instanceof LiftHeader) {
                    // LIFT 0.13: <field tag="..."> in header is the equivalent of <field-definition name="..."> in 0.15
                    if (attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.TAG_ATTRIBUTE) == null) {
                        throw new IllegalStateException(
                            "Attribute 'tag' expected on <field> in header (LIFT 0.13)"
                        );
                    }
                    frames.push(decoder.fieldDefinition(attributes));
                } else {
                    frames.push(decoder.field(top(), attributes));
                }
                break;
            case LiftVocabulary.VARIANT_LOCAL_NAME:
                frames.push(decoder.variant(top(), attributes));
                break;
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
                frames.push(decoder.example(top(), attributes));
                break;
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
                // annotation has no other possible child than form. An annotation can be on
                // AbstractExtensibleWithoutField object, field, trait, MultiText, or formContent (form or gloss).
                HasAnnotation parent;
                if (inTextDepth > 0) {
                    throw new IllegalStateException(
                        "Annotation cannot be in text element."
                    );
                } else if (currentForm() != null) {
                    parent = currentForm();
                } else if (!multiTextStack.isEmpty()) {
                    parent = multiTextStack.peek();
                } else if (top() instanceof HasAnnotation ha) {
                    parent = ha;
                } else {
                    throw new IllegalStateException(
                        "Expecting an object implementing HasAnnotation, found: " + top()
                    );
                }
                frames.push(decoder.annotation(parent, attributes));
                break;
            case LiftVocabulary.NOTE_LOCAL_NAME:
                frames.push(decoder.note(top(), attributes));
                break;
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
                frames.push(decoder.pronunciation(top(), attributes));
                break;
            case LiftVocabulary.FORM_LOCAL_NAME:
                // TODO in LiftEtymology, only one form element. To be checked after XML parsing.
                Form form = new Form(attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.LANG_ATTRIBUTE));
                decoder.addForm(multiTextStack.peek(), form);
                formStack.push(form);
                break;
            case LiftVocabulary.GLOSS_LOCAL_NAME: // in etymology and sense
                Form gloss = new Form(attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.LANG_ATTRIBUTE));
                formStack.push(gloss);
                decoder.gloss(frames.peek(), gloss);
                break;
            case LiftVocabulary.TEXT_LOCAL_NAME:
                inTextDepth++;
                break;
            case LiftVocabulary.SPAN_LOCAL_NAME:
                if (sb != null) {
                    currentForm().append(sb.toString());
                    sb = null;
                }
                String sLang = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.LANG_ATTRIBUTE);
                String sHref = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.HREF_ATTRIBUTE);
                String sClass = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.CLASS_ATTRIBUTE);
                TextSpan ts = new TextSpan();
                if (sLang != null) ts.setLang(sLang);
                if (sHref != null) ts.setHref(sHref);
                if (sClass != null) ts.setsClass(sClass);
                currentForm().append(ts);
                break;
            case LiftVocabulary.TRAIT_LOCAL_NAME:
                // on ExtensibleWithoutField or on grammatical-info, all implementing HasTrait.
                frames.push(decoder.trait(top(), attributes));
                break;
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
                // In a sense, a reversal, and the <main> of a reversal.
                if (top() instanceof HasGrammaticalInfo) {
                    frames.push(decoder.grammaticalInfo(top(), attributes));
                } else {
                    skipUnknownElement("", localName,
                        "is only allowed in <sense>, <subsense>, <reversal> and <main>");
                    return false;
                }
                break;
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                frames.push(decoder.media(top(), attributes));
                break;
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
                frames.push(decoder.illustration(top(), attributes));
                break;
            case LiftVocabulary.HEADER_LOCAL_NAME:
                frames.push(decoder.header());
                break;
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
                frames.push(decoder.range(attributes));
                break;
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
                Frame rangeElement = decoder.rangeElement(top(), attributes);
                if (rangeElement == null) {
                    // Declared twice: the decoder warned.
                    skipDepth = 1;
                    return false;
                }
                frames.push(rangeElement);
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
                frames.push(decoder.translation(top(), attributes));
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
                frames.push(decoder.reversal(top(), attributes));
                break;
            case LiftVocabulary.MAIN_LOCAL_NAME:
                frames.push(decoder.reversalMain(top(), attributes));
                break;
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME: // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
            case LiftVocabulary.USAGE_LOCAL_NAME:
                break;
            default:
                skipUnknownElement("", localName, "is not a LIFT element");
                return false;
        }
        return true;
    }

    /**
     * Second step of a start tag: if the element holds forms (or glosses), open the
     * MultiText they go into.
     */
    private void pushMultiText(String localName) {
        switch (localName) {
            case LiftVocabulary.FIELD_LOCAL_NAME:
                switch (top()) {
                    case LiftField f -> multiTextStack.push(f.getText());
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(fd.getDescription());
                    default -> throw new IllegalStateException();
                }
                break;
            // in the following cases ./form or ./gloss for sense
            // can appear directly as child element,
            // intermixed with other elements
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
            // etymology. Has both form and gloss...
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
            case LiftVocabulary.VARIANT_LOCAL_NAME:
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
            case LiftVocabulary.NOTE_LOCAL_NAME:
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
            // cases where we are entering a multitext object :
            // can contain only form element,
            // without other elements intermixed
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
                multiTextStack.push(top().getMainMultiText());
                break;
            // in the following cases formContent should
            // not be wired to the mainMultiText but to a secondary one.
            case LiftVocabulary.CITATION_LOCAL_NAME:
                multiTextStack.push(((LiftEntry) top()).getCitations());
                break;
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
                multiTextStack.push(((LiftSense) top()).getDefinition());
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                multiTextStack.push(((LiftFieldAndTraitDefinition) top()).getDescription());
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
                multiTextStack.push(((LiftTranslation) top()).getTranslation());
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
            case LiftVocabulary.MAIN_LOCAL_NAME:
                multiTextStack.push(((LiftReversal) top()).getForms());
                break;
            case LiftVocabulary.USAGE_LOCAL_NAME:
                if (top() instanceof LiftRelation rel) {
                    multiTextStack.push(rel.getUsage());
                } else {
                    throw new IllegalStateException(
                        "Expecting LiftRelation for usage, found: " + top()
                    );
                }
                break;
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
                // in header: elements header, range, range-element, field-definition
                switch (top()) {
                    case LiftHeader h -> multiTextStack.push(h.getDescription());
                    case FeatureSet r -> {
                        decoder.rangeText(r, r.getDescription());
                        multiTextStack.push(r.getDescription());
                    }
                    case Feature re -> multiTextStack.push(re.getDescription());
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(fd.getDescription());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
                // header only in range and range-element
                switch (top()) {
                    case FeatureSet r -> {
                        decoder.rangeText(r, r.getAbbrev());
                        multiTextStack.push(r.getAbbrev());
                    }
                    case Feature re -> multiTextStack.push(re.getAbbrev());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.LABEL_LOCAL_NAME:
                // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
                switch (top()) {
                    case FeatureSet r -> {
                        decoder.rangeText(r, r.getLabel());
                        multiTextStack.push(r.getLabel());
                    }
                    case Feature re -> multiTextStack.push(re.getLabel());
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(fd.getLabel());
                    case LiftIllustration i -> multiTextStack.push(i.getLabel());
                    case LiftMedia m -> multiTextStack.push(m.getLabel());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.LIFT_LOCAL_NAME:
            case LiftVocabulary.ENTRY_LOCAL_NAME:
            case LiftVocabulary.FORM_LOCAL_NAME:
            case LiftVocabulary.GLOSS_LOCAL_NAME:
            case LiftVocabulary.TEXT_LOCAL_NAME:
            case LiftVocabulary.SPAN_LOCAL_NAME:
            case LiftVocabulary.TRAIT_LOCAL_NAME:
            case LiftVocabulary.HEADER_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
            case LiftVocabulary.RELATION_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                break;
            default:
                LOGGER.warning(
                    "No multitext mapping for element <" +
                        localName +
                        "> (startElement, second switch)."
                );
                break;
        }
    }

    @Override
    public void endDocument()
        throws SAXException {
        decoder.endDocument();
        super.endDocument();
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
        if (skipDepth > 0) {
            skipDepth--;
            super.endElement(uri, localName, qName);
            return;
        }
        if (DEBUGGING) LOGGER.info(
            "end element: " +
                localName +
                "; frame stack size: " +
                frames.size() +
                "; Text stack size: " +
                multiTextStack.size()
        );
        try {
            close(localName);
        } catch (RuntimeException e) {
            throw located(localName, e);
        }
        super.endElement(uri, localName, qName);
    }

    private void close(String localName) {
        // First: leave the MultiText the element opened, if any.
        switch (localName) {
            case LiftVocabulary.FIELD_LOCAL_NAME:
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
            case LiftVocabulary.VARIANT_LOCAL_NAME:
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
            case LiftVocabulary.NOTE_LOCAL_NAME:
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME: // duplicate constant "description"
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME: // in range, range-element, illustration or media
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
            case LiftVocabulary.MAIN_LOCAL_NAME:
            case LiftVocabulary.USAGE_LOCAL_NAME:
                multiTextStack.pop();
                break;
            case LiftVocabulary.TEXT_LOCAL_NAME:
            case LiftVocabulary.FORM_LOCAL_NAME:
            case LiftVocabulary.ENTRY_LOCAL_NAME:
            case LiftVocabulary.SPAN_LOCAL_NAME:
            case LiftVocabulary.LIFT_LOCAL_NAME:
            case LiftVocabulary.TRAIT_LOCAL_NAME:
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
            case LiftVocabulary.GLOSS_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.HEADER_LOCAL_NAME:
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
            case LiftVocabulary.RELATION_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                break;
            default:
                LOGGER.warning(
                    "Unknown element </" +
                        localName +
                        "> (endElement, first switch)."
                );
                return;
        }

        // Second: build the component the element created, or close the text model.
        switch (localName) {
            case LiftVocabulary.ENTRY_LOCAL_NAME:
                // The entry is complete by now - senses, examples, traits and all - and
                // was built detached, so none of the builds along the way registered
                // anything: this one registers the whole subtree.
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
            case LiftVocabulary.RELATION_LOCAL_NAME:
            case LiftVocabulary.FIELD_LOCAL_NAME:
            case LiftVocabulary.VARIANT_LOCAL_NAME:
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
            case LiftVocabulary.NOTE_LOCAL_NAME:
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
            case LiftVocabulary.TRAIT_LOCAL_NAME:
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
            case LiftVocabulary.MAIN_LOCAL_NAME:
                decoder.close(frames.pop());
                break;
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
                decoder.closeRange(frames.pop());
                break;
            case LiftVocabulary.HEADER_LOCAL_NAME:
                decoder.endHeader();
                frames.pop();
                break;
            case LiftVocabulary.FORM_LOCAL_NAME:
            case LiftVocabulary.GLOSS_LOCAL_NAME:
                formStack.pop();
                break;
            case LiftVocabulary.TEXT_LOCAL_NAME:
                if (sb != null) {
                    currentForm().append(sb.toString());
                    sb = null;
                }
                inTextDepth--;
                break;
            case LiftVocabulary.SPAN_LOCAL_NAME:
                if (sb != null) {
                    currentForm().append(sb.toString());
                    sb = null;
                }
                currentForm().pop();
                break;
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LIFT_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
            case LiftVocabulary.USAGE_LOCAL_NAME:
                break;
            default:
                LOGGER.warning(
                    "Unknown element </" +
                        localName +
                        "> (endElement, second switch)."
                );
                break;
        }
    }
}
