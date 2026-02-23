package fr.cnrs.lacito.liftapi.xml;
import java.util.ArrayDeque;

import java.util.Deque;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftHeaderFieldDefinition;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRange;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftFactory;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftReversal;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.MultiText;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.TextSpan;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

/**
 * SAX handler for turning a LIFT XML files into a set of objects.
 */
public final class LiftSaxHandler extends DefaultHandler {

    private final static boolean DEBUGGING = false;

    private static final Logger LOGGER = Logger.getLogger(LiftDictionary.class.getName());

    private boolean inText = false; // inside a text element.
    private boolean inGrammaticalInfo = false; // (for trait)
    
    private final LiftFactory liftFactory;

    private Deque<AbstractLiftRoot> elementStack = new ArrayDeque<>();
	private Deque<MultiText> multiTextStack = new ArrayDeque<>();

    private Form currentFormContent; // contains @lang, text, and annotation
    private StringBuffer sb;

    public LiftSaxHandler(LiftFactory lf) {
        this.liftFactory = lf;
    }

    public LiftFactory getFactory() {
        return liftFactory;
    };

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inText) { // we are in text
            if (sb == null) sb = new StringBuffer();
            sb.append(ch, start, length);
        }
        super.characters(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (DEBUGGING) LOGGER.info("start element: " + localName +
                                   "; object stack size: " + elementStack.size() +
                                   "; Text stack size: " + multiTextStack.size());
        
        // First switch: create object
        switch (localName) {
            case LiftVocabulary.LIFT_LOCAL_NAME:
                String version = attributes.getValue(LiftVocabulary.VERSION_ATTRIBUTE);
                //if (!version.equals("15")) throw new UnsupportedVersionException("The lift-api read dictionary with version " + LiftVocabulary.CURRENT_LIFT_VERSION + "; found: " + version);
                // TODO Parameter the reader to take into account v. 13 features
                liftFactory.setLiftVersion(version);
                liftFactory.setLiftProducer(attributes.getValue(LiftVocabulary.PRODUCER_ATTRIBUTE));
                break;
            case LiftVocabulary.ENTRY_LOCAL_NAME:
                elementStack.push(liftFactory.createEntry(attributes));
                break;
            case LiftVocabulary.SUBSENSE_LOCAL_NAME:
            case LiftVocabulary.SENSE_LOCAL_NAME:
                LiftSense ls = switch(elementStack.peek()) {
                    case LiftEntry e -> liftFactory.createSense(attributes, e);
                    case LiftSense s -> liftFactory.createSense(attributes, s);
                    default -> throw new IllegalStateException("Expecting a sense holding object (Entry or Sense), found: " + elementStack.peek().toString());
                };
                elementStack.push(ls);
                break;
            case LiftVocabulary.ETYMOLOGY_LOCAL_NAME:
                elementStack.push(liftFactory.createEtymology(attributes, (LiftEntry)elementStack.peek()));
                break;
            case LiftVocabulary.RELATION_LOCAL_NAME: // only entry, sens, variant may have relation (and implement HasRelations)
                elementStack.push(liftFactory.createRelation(attributes, (HasRelations)elementStack.peek()));
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                elementStack.push(liftFactory.create_field_definition(attributes, (LiftHeader)elementStack.peek()));
                break;
            case LiftVocabulary.FIELD_LOCAL_NAME:
                if (elementStack.peek() instanceof AbstractExtensibleWithField a) {
                    // TODO : in the factory, if version == 13, then look for "tag" and not for "name"
                    elementStack.push(liftFactory.createField(attributes, a));
                // TODO : check code below:
                // else if (elementStack.peek() instanceof LiftHeader h) {
                //   if (factory.getLiftVersion().equals("0.13")) {
                //       ... TODO
                //   }
                //}
                } else {
                    throw new IllegalStateException("Expecting a field-holding object, found: " + elementStack.peek().toString());
                }
                break;
            case LiftVocabulary.VARIANT_LOCAL_NAME: // Example and Variant are at the same time main object and support of multitext
                elementStack.push(liftFactory.createVariant(attributes, (LiftEntry)elementStack.peek()));
                break;
            case LiftVocabulary.EXAMPLE_LOCAL_NAME:
                elementStack.push(liftFactory.createExample(attributes, (LiftSense)elementStack.peek()));
                break;
            case LiftVocabulary.ANNOTATION_LOCAL_NAME:
                // annotation has no other possible child than form. An annotation can be on
                // AbstractExtensibleWithoutField object, field, trait, MultiText, or formContent (form or gloss).
                HasAnnotation parent = null;
                if (inText) {
                    throw new IllegalStateException("Annotation cannot be in text element.");
                } else if (currentFormContent != null) {
                    parent = currentFormContent;
                } else if (!multiTextStack.isEmpty() && multiTextStack.peek() instanceof HasAnnotation haMt) {
                    parent = haMt;
                } else {
                    if (elementStack.peek() instanceof HasAnnotation ha) {
                        parent = ha;
                    } else {
                        throw new IllegalStateException("Expecting an object implementing HasAnnotation, found: " + elementStack.peek().toString());
                    }
                }
                elementStack.push(liftFactory.createAnnotation(attributes, parent));
                break;
            case LiftVocabulary.NOTE_LOCAL_NAME:
                if (elementStack.peek() instanceof AbstractNotable note_parent)
                    elementStack.push(liftFactory.create_note(attributes, note_parent));
                else
                    throw new IllegalStateException("Expecting an AbstractNotable, found: " + elementStack.peek().toString());
                break;
            case LiftVocabulary.PRONUNCIATION_LOCAL_NAME:
                HasPronunciation p_parent = switch (elementStack.peek()) {
                    case HasPronunciation hp -> hp;
                    default -> throw new IllegalStateException("Should implement HasPronunciation; found: " + elementStack.peek().toString());
                };
                elementStack.push(liftFactory.createPronounciation(attributes, p_parent));
                break;
            // next two cases: ambiguous tags: either inside multitext content or outside
            case LiftVocabulary.FORM_LOCAL_NAME:
                // TODO in LiftEtymology, only one form element. To be checked after XML parsing.
                String lang = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.LANG_ATTRIBUTE);
                currentFormContent = liftFactory.createText(lang);
                multiTextStack.peek().add(currentFormContent);
                break;
            case LiftVocabulary.GLOSS_LOCAL_NAME: // in etymology and sense
                // TODO : essayer de grouper avec le précédent
                String glossLang = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.LANG_ATTRIBUTE);
                currentFormContent = liftFactory.createText(glossLang);
                //currentMulti.add(currentLiftText);
                if (elementStack.peek() instanceof LiftEtymology le) {
                    le.addGloss(currentFormContent);
                } else if (elementStack.peek() instanceof LiftSense ls2) {
                    ls2.addGloss(currentFormContent);
                } else {
                    throw new IllegalStateException("gloss element is allowed in etymology and sense; found: " + elementStack.peek().toString());
                }
                break;
            // Dealing with multitex
            case LiftVocabulary.TEXT_LOCAL_NAME:
                inText = true;
                break;
            case LiftVocabulary.SPAN_LOCAL_NAME:
                if (sb != null) {
                    currentFormContent.append(sb.toString());
                    sb = null;
                }
                String sLang = attributes.getValue(LiftVocabulary.LIFT_URI, "lang");
                String sHref = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
                String sClass = attributes.getValue(LiftVocabulary.LIFT_URI, "class");
                TextSpan ts = liftFactory.createTextSpan();
                if (sLang != null) ts.setLang(sLang);
                if (sHref != null) ts.setHref(sHref);
                if (sClass != null) ts.setsClass(sClass);
                currentFormContent.append(ts);
                break;
            case LiftVocabulary.TRAIT_LOCAL_NAME:
                // on ExtensibleWithoutField or on grammatical-info, all implementing HasTrait.
                if (elementStack.peek() instanceof HasTrait s) {
                    if (inGrammaticalInfo) {
                        s = ((LiftSense)s).getGrammaticalInfo().orElseThrow();
                    }
                    LiftTrait trait = liftFactory.createTrait(attributes, s);
                    elementStack.push(trait);
                } else {
                    throw new IllegalStateException("Expecting an objet implementing HasTrait, found: " + elementStack.peek().toString());
                }
                break;
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
                inGrammaticalInfo = true;
                if (elementStack.peek() instanceof LiftSense s) {
                    s.setGrammaticalInfo(attributes.getValue(LiftVocabulary.LIFT_URI, "value"));
                } else {
                    throw new IllegalStateException("LiftSense expected. Found: " + elementStack.peek().toString());
                }
                break;
            // <media href="..." >
            //   <label>
            //     <form>...</form>
            //   </label>
            // </media>
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                elementStack.push(liftFactory.createMedia(attributes, (LiftPronunciation)elementStack.peek()));
                break;
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
                elementStack.push(liftFactory.create_illustration(attributes, (LiftSense)elementStack.peek()));
                break;
            case LiftVocabulary.HEADER_LOCAL_NAME:
                elementStack.push(liftFactory.createHeader());
                break;
            case LiftVocabulary.HEADER_RANGE_LOCAL_NAME:
                elementStack.push(liftFactory.create_range(attributes, (LiftHeader)elementStack.peek()));
                break;
            case LiftVocabulary.HEADER_RANGE_ELEMENT_LOCAL_NAME:
                elementStack.push(liftFactory.create_range_element(attributes, (LiftHeaderRange)elementStack.peek()));
                break;
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME: // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
                 break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftSense s_rev) {
                    elementStack.push(liftFactory.createReversal(attributes, s_rev));
                } else {
                    throw new IllegalStateException("Expecting a LiftSense for reversal, found: " + elementStack.peek().toString());
                }
                break;
            case LiftVocabulary.MAIN_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal parentRev) {
                    elementStack.push(liftFactory.createReversalMain(parentRev));
                } else {
                    throw new IllegalStateException("Expecting a LiftReversal for main, found: " + elementStack.peek().toString());
                }
                break;
            case LiftVocabulary.USAGE_LOCAL_NAME:
                 break;
            default:
                throw new IllegalStateException("Unknown start element: " + localName);
                //break;
        }
        // end of first switch



        // Second switch:
        // If the following element can be form or gloss,
        // we register a MultiText object
        switch (localName) {
            case LiftVocabulary.ABREVIATION_LOCAL_NAME:
            // in a range or a range element
                switch (elementStack.peek()) {
                    case LiftHeaderRange r ->  multiTextStack.push(r.getAbbrev());
                    case LiftHeaderRangeElement re -> multiTextStack.push(re.getAbbrev());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.FIELD_LOCAL_NAME:
                switch (elementStack.peek()) {
                    case LiftField f ->  multiTextStack.push(f.getText());
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
                multiTextStack.push(((LiftEntry)elementStack.peek()).getCitations());
                break;
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
                multiTextStack.push(((LiftSense)elementStack.peek()).getDefinition());
                break;
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                multiTextStack.push(((LiftHeaderFieldDefinition)elementStack.peek()).getDescription());
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
                String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
                if (type == null) type = LiftExample.DEFAULT_TRANSLATION_TYPE; // TODO
                if (elementStack.peek() instanceof LiftExample e) {
                    multiTextStack.push(e.create_translation(type));
                } else {
                    throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.REVERSAL_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal rev) {
                    multiTextStack.push(rev.getForms());
                } else {
                    throw new IllegalStateException("Expecting LiftReversal on stack for reversal multitext");
                }
                break;
            case LiftVocabulary.MAIN_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftReversal revMain) {
                    multiTextStack.push(revMain.getForms());
                } else {
                    throw new IllegalStateException("Expecting LiftReversal on stack for main multitext");
                }
                break;
            case LiftVocabulary.USAGE_LOCAL_NAME:
                if (elementStack.peek() instanceof LiftRelation rel) {
                    multiTextStack.push(rel.getUsage());
                } else {
                    throw new IllegalStateException("Expecting LiftRelation for usage, found: " + elementStack.peek().toString());
                }
                break;
            case LiftVocabulary.HEADER_DESCRIPTION_LOCAL_NAME:
                // in header: elements header, range, range-element, field-definition
                switch(elementStack.peek()) {
                    case LiftHeader h -> multiTextStack.push(h.getDescription());
                    case LiftHeaderRange r -> multiTextStack.push(r.getDescription());
                    case LiftHeaderRangeElement re -> multiTextStack.push(re.getDescription());
                    case LiftHeaderFieldDefinition fd -> multiTextStack.push(fd.getDescription());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.HEADER_RANGE_ABBREV_LOCAL_NAME:
            // header only in range and range-element
                switch(elementStack.peek()) {
                    case LiftHeaderRange r -> multiTextStack.push(r.getAbbrev());
                    case LiftHeaderRangeElement re -> multiTextStack.push(re.getAbbrev());
                    default -> throw new IllegalStateException();
                }
                break;
            case LiftVocabulary.LABEL_LOCAL_NAME:
            // label is on range-element, range, field-definition, and "URLRef-content" i.e. illustration, media
                switch(elementStack.peek()) {
                    case LiftHeaderRange r -> multiTextStack.push(r.getLabel());
                    case LiftHeaderRangeElement re -> multiTextStack.push(re.getLabel());
                    case LiftHeaderFieldDefinition fd -> multiTextStack.push(fd.getLabel());
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
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
            case LiftVocabulary.RELATION_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                break;
            default:
                throw new IllegalStateException("Unknown case (second start element switch): " + localName);              
                //break;
        }
        // end of second switch

        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (DEBUGGING) LOGGER.info("end element: " + localName +
                                "; object stack size: " + elementStack.size() +
                                "; Text stack size: " + multiTextStack.size());

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
            case LiftVocabulary.ABREVIATION_LOCAL_NAME:
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
            case LiftVocabulary.GLOSS_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.HEADER_LOCAL_NAME:
            case LiftVocabulary.ILLUSTRATION_LOCAL_NAME:
            case LiftVocabulary.RELATION_LOCAL_NAME:
            case LiftVocabulary.MEDIA_LOCAL_NAME:
                break;
            default:
                throw new IllegalStateException("Unknown case (endElement, first switch): " + localName);
                //break;
        }
        // end of first switch

        // second switch
        switch(localName) {
            case LiftVocabulary.ENTRY_LOCAL_NAME:
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
            case LiftVocabulary.HEADER_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELD_DEFINITION_LOCAL_NAME:
                elementStack.pop();
                break;
            case LiftVocabulary.FORM_LOCAL_NAME:
            case LiftVocabulary.GLOSS_LOCAL_NAME:
                 currentFormContent = null;
                 break;
            case LiftVocabulary.TEXT_LOCAL_NAME:
                if (sb != null) {
                    currentFormContent.append(sb.toString());
                    sb = null;
                }
                inText = false;
                break;
            case LiftVocabulary.SPAN_LOCAL_NAME:
                if (sb != null) {
                    currentFormContent.append(sb.toString());
                    sb = null;
                }
                currentFormContent.pop();
                break;
            case LiftVocabulary.GRAM_INFO_LOCAL_NAME:
                inGrammaticalInfo = false; // that flag is important (for trait) // TODO remove
                break;
            case LiftVocabulary.TRANSLATION_LOCAL_NAME:
            case LiftVocabulary.LEXICAL_UNIT_LOCAL_NAME:
            case LiftVocabulary.CITATION_LOCAL_NAME:
            case LiftVocabulary.DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LABEL_LOCAL_NAME:
            case LiftVocabulary.HEADER_RANGES_LOCAL_NAME:
            case LiftVocabulary.HEADER_FIELDS_DEFINITION_LOCAL_NAME:
            case LiftVocabulary.LIFT_LOCAL_NAME:
                 break;
            default: 
                throw new IllegalStateException("Unknwon end element (endElement, second switch): " + localName);
                //break;
        }
        // end of second switch

        super.endElement(uri, localName, qName);
    }
}
