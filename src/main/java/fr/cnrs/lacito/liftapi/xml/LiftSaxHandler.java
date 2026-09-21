package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
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
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler for turning a LIFT XML files into a set of objects,
 * using the LiftXMLFactory to create the objects.
 */
public final class LiftSaxHandler extends DefaultHandler {

    private static final boolean DEBUGGING = false;

    private static final Logger LOGGER = Logger.getLogger(
        LiftSaxHandler.class.getName()
    );

    /** Nesting depth of {@code <text>} elements (0 when outside any). */
    private int inTextDepth = 0;

    /** Nesting depth of {@code <grammatical-info>} elements (matters for traits). */
    private int inGrammaticalInfoDepth = 0;

    /**
     * Depth of the subtree currently being skipped because its root element is not
     * part of the LIFT vocabulary. 0 when not skipping.
     */
    private int skipDepth = 0;

    private final LiftXMLFactory liftXMLFactory;

    private final boolean strict;

    private Deque<AbstractLiftRoot> elementStack = new ArrayDeque<>();
    private Deque<MultiText> multiTextStack = new ArrayDeque<>();

    /**
     * The open {@code <form>} / {@code <gloss>} elements, innermost first.
     *
     * This is a stack rather than a single field because the schema allows an
     * {@code <annotation>} - itself a multitext - inside a {@code <form>}: closing the
     * inner form must restore the outer one, not clear it.
     */
    private Deque<Form> formStack = new ArrayDeque<>();

    private StringBuffer sb;

    public LiftSaxHandler(LiftXMLFactory lf) {
        this(lf, false);
    }

    /**
     * @param strict when {@code true}, recoverable parse errors abort the parse
     *        instead of only being logged.
     */
    public LiftSaxHandler(LiftXMLFactory lf, boolean strict) {
        this.liftXMLFactory = lf;
        this.strict = strict;
    }

    public LiftXMLFactory getFactory() {
        return liftXMLFactory;
    }

    /** The {@code <form>} or {@code <gloss>} currently open, or {@code null}. */
    private Form currentForm() {
        return formStack.peek();
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
        LOGGER.severe(message);
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
     * Log an element the LIFT vocabulary does not cover and skip its whole subtree.
     *
     * Aborting the parse instead would make any file with a single unexpected
     * element unreadable - including the {@code lift-ranges} documents this package
     * writes itself.
     */
    private void skipUnknownElement(String localName, String where) {
        LOGGER.warning(
            "Skipping unknown LIFT element <" + localName + "> (" + where + ")."
        );
        skipDepth = 1;
    }

    @Override
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes
    ) throws SAXException {
        if (skipDepth > 0) {
            skipDepth++;
            super.startElement(uri, localName, qName, attributes);
            return;
        }
        if (DEBUGGING) LOGGER.info(
            "start element: " +
                localName +
                "; object stack size: " +
                elementStack.size() +
                "; Text stack size: " +
                multiTextStack.size()
        );

        // First switch: create object
        switch (localName) {
            case LiftVocabulary.LIFT_LOCAL_NAME:
                String version = attributes.getValue(
                    LiftVocabulary.VERSION_ATTRIBUTE
                );
                if (version == null) {
                    throw new UnsupportedVersionException(
                        "The <lift> element has no 'version' attribute; " +
                            "supported versions are 0.13 and 0.15"
                    );
                }
                switch (version) {
                    case "0.13" -> liftXMLFactory.setLiftVersion(LiftVersion.V0_13);
                    case "0.15" -> liftXMLFactory.setLiftVersion(LiftVersion.V0_15);
                    default -> throw new UnsupportedVersionException(
                        "Cannot read lift dictionary with version '" +
                            version +
                            "'; supported versions are 0.13 and 0.15"
                    );
                }
                liftXMLFactory.setLiftProducer(
                    attributes.getValue(LiftVocabulary.PRODUCER_ATTRIBUTE)
                );
                break;
            case LiftVocabulary.ENTRY_LOCAL_NAME:
                elementStack.push(liftXMLFactory.createEntry(attributes));
                break;
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
                LiftSense ls = switch (elementStack.peek()) {
                    case LiftEntry e -> liftXMLFactory.createSense(
                        attributes,
                        e
                    );
                    case LiftSense s -> liftXMLFactory.createSense(
                        attributes,
                        s
                    );
                    default -> throw new IllegalStateException(
                        "Expecting a sense holding object (Entry or Sense), found: " +
                            elementStack.peek().toString()
                    );
                };
                elementStack.push(ls);
                break;
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createEtymology(
                        attributes,
                        (LiftEntry) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.RELATION_LOCAL_NAME: // only entry, sens, variant may have relation (and implement HasRelations)
                elementStack.push(
                    liftXMLFactory.createRelation(
                        attributes,
                        (HasRelations) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createFieldOrTraitDefinition(
                        attributes,
                        (LiftHeader) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.FIELD_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftHeader h) {
                    // LIFT 0.13: <field tag="..."> in header is the equivalent of <field-definition name="..."> in 0.15
                    String tag = attributes.getValue(
                        LiftVocabulary.LIFT_URI,
                        LiftVocabulary.TAG_ATTRIBUTE
                    );
                    if (tag == null) throw new IllegalStateException(
                        "Attribute 'tag' expected on <field> in header (LIFT 0.13)"
                    );
                    elementStack.push(
                        liftXMLFactory.createFieldOrTraitDefinition(attributes, h)
                    );
                } else if (
                    elementStack.peek() instanceof AbstractExtensibleWithField a
                ) {
                    elementStack.push(
                        liftXMLFactory.createField(attributes, a)
                    );
                } else {
                    throw new IllegalStateException(
                        "Expecting a field-holding object, found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            case LiftVocabulary.VARIANT_LOCAL_NAME: // Example and Variant are at the same time main object and support of multitext
                elementStack.push(
                    liftXMLFactory.createVariant(
                        attributes,
                        (LiftEntry) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createExample(
                        attributes,
                        (LiftSense) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
                // annotation has no other possible child than form. An annotation can be on
                // AbstractExtensibleWithoutField object, field, trait, MultiText, or formContent (form or gloss).
                HasAnnotation parent = null;
                if (inTextDepth > 0) {
                    throw new IllegalStateException(
                        "Annotation cannot be in text element."
                    );
                } else if (currentForm() != null) {
                    parent = currentForm();
                } else if (
                    !multiTextStack.isEmpty() &&
                    multiTextStack.peek() instanceof HasAnnotation haMt
                ) {
                    parent = haMt;
                } else {
                    if (elementStack.peek() instanceof HasAnnotation ha) {
                        parent = ha;
                    } else {
                        throw new IllegalStateException(
                            "Expecting an object implementing HasAnnotation, found: " +
                                elementStack.peek().toString()
                        );
                    }
                }
                elementStack.push(
                    liftXMLFactory.createAnnotation(attributes, parent)
                );
                break;
            case LiftVocabulary.NOTE_LOCAL_NAME:
                if (
                    elementStack.peek() instanceof AbstractNotable note_parent
                ) elementStack.push(
                    liftXMLFactory.createNoteWithAttributes(attributes, note_parent)
                );
                else throw new IllegalStateException(
                    "Expecting an AbstractNotable, found: " +
                        elementStack.peek().toString()
                );
                break;
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
                HasPronunciation p_parent = switch (elementStack.peek()) {
                    case HasPronunciation hp -> hp;
                    default -> throw new IllegalStateException(
                        "Should implement HasPronunciation; found: " +
                            elementStack.peek().toString()
                    );
                };
                elementStack.push(
                    liftXMLFactory.createPronounciation(attributes, p_parent)
                );
                break;
            // next two cases: ambiguous tags: either inside multitext content or outside
            case LiftVocabulary.FORM_LOCAL_NAME:
                // TODO in LiftEtymology, only one form element. To be checked after XML parsing.
                String lang = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.LANG_ATTRIBUTE
                );
                Form form = liftXMLFactory.createText(lang);
                multiTextStack.peek().add(form);
                formStack.push(form);
                break;
            case LiftVocabulary.GLOSS_LOCAL_NAME: // in etymology and sense
                // TODO : essayer de grouper avec le précédent
                String glossLang = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.LANG_ATTRIBUTE
                );
                Form gloss = liftXMLFactory.createText(glossLang);
                formStack.push(gloss);
                if (elementStack.peek() instanceof LiftEtymology le) {
                    le.addGloss(gloss);
                } else if (elementStack.peek() instanceof LiftSense ls2) {
                    ls2.addGloss(gloss);
                } else {
                    throw new IllegalStateException(
                        "gloss element is allowed in etymology and sense; found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            // Dealing with multitex
            case LiftVocabulary.TEXT_LOCAL_NAME:
                inTextDepth++;
                break;
            case LiftVocabulary.SPAN_LOCAL_NAME:
                if (sb != null) {
                    currentForm().append(sb.toString());
                    sb = null;
                }
                String sLang = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.LANG_ATTRIBUTE
                );
                String sHref = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.HREF_ATTRIBUTE
                );
                String sClass = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.CLASS_ATTRIBUTE
                );
                TextSpan ts = liftXMLFactory.createTextSpan();
                if (sLang != null) ts.setLang(sLang);
                if (sHref != null) ts.setHref(sHref);
                if (sClass != null) ts.setsClass(sClass);
                currentForm().append(ts);
                break;
            case LiftVocabulary.TRAIT_LOCAL_NAME:
                // on ExtensibleWithoutField or on grammatical-info, all implementing HasTrait.
                if (elementStack.peek() instanceof HasTrait s) {
                    if (inGrammaticalInfoDepth > 0) {
                        s = ((LiftSense) s).getGrammaticalInfo().orElseThrow();
                    }
                    LiftTrait trait = liftXMLFactory.createTrait(attributes, s);
                    elementStack.push(trait);
                } else {
                    throw new IllegalStateException(
                        "Expecting an objet implementing HasTrait, found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftSense s) {
                    inGrammaticalInfoDepth++;
                    liftXMLFactory.setGrammaticalInfo(
                        s,
                        attributes.getValue(
                            LiftVocabulary.LIFT_URI,
                            LiftVocabulary.VALUE_ATTRIBUTE
                        )
                    );
                } else {
                    // The schema also allows grammatical-info inside a reversal, which
                    // the model does not represent; skip it rather than fail the parse.
                    skipUnknownElement(
                        localName,
                        "grammatical-info outside a sense"
                    );
                    super.startElement(uri, localName, qName, attributes);
                    return;
                }
                break;
            // <media href="..." >
            //   <label>
            //     <form>...</form>
            //   </label>
            // </media>
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createMedia(
                        attributes,
                        (LiftPronunciation) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createIllustration(
                        attributes,
                        (LiftSense) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.HEADER_LOCAL_NAME:
                elementStack.push(liftXMLFactory.getHeader());
                break;
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createRange(
                        attributes,
                        (LiftHeader) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
                elementStack.push(
                    liftXMLFactory.createRangeElement(
                        attributes,
                        (FeatureSet) elementStack.peek()
                    )
                );
                break;
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME: // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftSense s_rev) {
                    elementStack.push(
                        liftXMLFactory.createReversal(attributes, s_rev)
                    );
                } else {
                    throw new IllegalStateException(
                        "Expecting a LiftSense for reversal, found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            case LiftVocabulary.MAIN_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal parentRev) {
                    elementStack.push(
                        liftXMLFactory.createReversalMain(parentRev)
                    );
                } else {
                    throw new IllegalStateException(
                        "Expecting a LiftReversal for main, found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            case LiftVocabulary.USAGE_LOCAL_NAME:
                break;
            default:
                skipUnknownElement(localName, "startElement, first switch");
                super.startElement(uri, localName, qName, attributes);
                return;
        }
        // end of first switch

        // Second switch:
        // If the following element can be form or gloss,
        // we register a MultiText object
        switch (localName) {
            case LiftVocabulary.FIELD_LOCAL_NAME:
                switch (elementStack.peek()) {
                    case LiftField f -> multiTextStack.push(f.getText());
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(
                        fd.getDescription()
                    );
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
                multiTextStack.push(elementStack.peek().getMainMultiText());
                break;
            // in the following cases formContent should
            // not be wired to the mainMultiText but to a secondary one.
            case LiftVocabulary.CITATION_LOCAL_NAME:
                multiTextStack.push(
                    ((LiftEntry) elementStack.peek()).getCitations()
                );
                break;
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
                multiTextStack.push(
                    ((LiftSense) elementStack.peek()).getDefinition()
                );
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                multiTextStack.push(
                    (
                        (LiftFieldAndTraitDefinition) elementStack.peek()
                    ).getDescription()
                );
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
                String type = attributes.getValue(
                    LiftVocabulary.LIFT_URI,
                    LiftVocabulary.TYPE_ATTRIBUTE
                );
                if (type == null) type = LiftExample.DEFAULT_TRANSLATION_TYPE;
                Feature typeObject = liftXMLFactory.getTranslationType(type);
                //if (type == null) type = LiftExample.DEFAULT_TRANSLATION_TYPE; // TODO
                if (elementStack.peek() instanceof LiftExample e) {
                    multiTextStack.push(e.createTranslation(typeObject));
                } else {
                    throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal rev) {
                    multiTextStack.push(rev.getForms());
                } else {
                    throw new IllegalStateException(
                        "Expecting LiftReversal on stack for reversal multitext"
                    );
                }
                break;
            case LiftVocabulary.MAIN_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal revMain) {
                    multiTextStack.push(revMain.getForms());
                } else {
                    throw new IllegalStateException(
                        "Expecting LiftReversal on stack for main multitext"
                    );
                }
                break;
            case LiftVocabulary.USAGE_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftRelation rel) {
                    multiTextStack.push(rel.getUsage());
                } else {
                    throw new IllegalStateException(
                        "Expecting LiftRelation for usage, found: " +
                            elementStack.peek().toString()
                    );
                }
                break;
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
                // in header: elements header, range, range-element, field-definition
                switch (elementStack.peek()) {
                    case LiftHeader h -> multiTextStack.push(
                        h.getDescription()
                    );
                    case FeatureSet r -> multiTextStack.push(
                        r.getDescription()
                    );
                    case Feature re -> multiTextStack.push(
                        re.getDescription()
                    );
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(
                        fd.getDescription()
                    );
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
                // header only in range and range-element
                switch (elementStack.peek()) {
                    case FeatureSet r -> multiTextStack.push(
                        r.getAbbrev()
                    );
                    case Feature re -> multiTextStack.push(
                        re.getAbbrev()
                    );
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.LABEL_LOCAL_NAME:
                // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
                switch (elementStack.peek()) {
                    case FeatureSet r -> multiTextStack.push(r.getLabel());
                    case Feature re -> multiTextStack.push(
                        re.getLabel()
                    );
                    case LiftFieldAndTraitDefinition fd -> multiTextStack.push(
                        fd.getLabel()
                    );
                    case LiftIllustration i -> multiTextStack.push(
                        i.getLabel()
                    );
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
        // end of second switch

        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endDocument()
        throws SAXException {
            liftXMLFactory.endDocument();
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
                "; object stack size: " +
                elementStack.size() +
                "; Text stack size: " +
                multiTextStack.size()
        );

        // First switch
        switch (localName) {
            // cases where we are leaving a multitext object
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
                multiTextStack.pop();
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
            case LiftVocabulary.MAIN_LOCAL_NAME:
                multiTextStack.pop();
                elementStack.pop();
                break;
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
        // end of first switch

        // second switch
        switch (localName) {
            case LiftVocabulary.ENTRY_LOCAL_NAME:
                LiftEntry e = (LiftEntry) elementStack.pop();
                liftXMLFactory.addEntryToDictionary(e);
                break;
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
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
            case LiftVocabulary.TRAIT_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                elementStack.pop();
                break;
            case LiftVocabulary.HEADER_LOCAL_NAME:
                liftXMLFactory.endHeader();
                elementStack.pop();
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
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
                inGrammaticalInfoDepth--; // that flag is important (for trait)
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LIFT_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
            case LiftVocabulary.MAIN_LOCAL_NAME:
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
        // end of second switch

        super.endElement(uri, localName, qName);
    }
}
