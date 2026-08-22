package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.LiftVersion;
import fr.cnrs.lacito.liftapi.model.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.xml.sax.Attributes;

public final class LiftXMLFactoryNew {

    private LiftVersion liftVersion;

    public LiftVersion getLiftVersion() {
        return liftVersion;
    }

    public void setLiftVersion(LiftVersion liftVersion) {
        this.liftVersion = liftVersion;
    }

    private String liftProducer;

    public String getLiftProducer() {
        return liftProducer;
    }

    public void setLiftProducer(String liftProducer) {
        this.liftProducer = liftProducer;
    }

    protected LiftHeader header = new LiftHeader();

    private LiftDictionaryRegistry registry;

    public LiftXMLFactoryNew(LiftDictionaryRegistry registry) {
        this.registry = registry;
    }

    public LiftHeader createHeader() {
        this.header = new LiftHeader();
        return this.header;
    }

    public void addEntryToDictionary(LiftEntry entry) {
        registry.addToDictionaryLowLevel(entry);
    }

    public LiftEntry createEntry(Attributes attributes) {
        LiftEntry entry = new LiftEntry();
        populateWithAttribute(entry, attributes);
        return entry;
    }

    public LiftSense createSense(Attributes attributes, LiftSense s) {
        LiftSense sense = createSense(attributes);
        s.addSense(sense);
        HasSense parent = s.getParent();
        while (parent instanceof LiftSense parentSense) {
            parent = parentSense.getParent();
        }
        sense.setParentEntry((LiftEntry) parent);
        return sense;
    }

    public LiftSense createSense(Attributes attributes, LiftEntry e) {
        LiftSense sense = createSense(attributes);
        e.addSense(sense);
        sense.setParentEntry(e);
        return sense;
    }

    private LiftSense createSense(Attributes attributes) {
        LiftSense sense = new LiftSense();
        populateWithAttribute(sense, attributes);
        return sense;
    }

    public LiftReversal createReversal(Attributes attributes, LiftSense sense) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException();
        if (!header.getNoteTypeManager().hasRangeElements(type)) {
            header.getNoteTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement element = header.getNoteTypeManager().getRangeElement(type);
        LiftReversal reversal = new LiftReversal(element);
        sense.addReversal(reversal);
        return reversal;
    }

    public LiftReversal createReversalMain(LiftReversal parent) {
        // TODO deal with null or ""...
        LiftReversal main = new LiftReversal(null);
        parent.setMain(main);
        return main;
    }

    public LiftEtymology createEtymology(
        Attributes attributes,
        LiftEntry parent
    ) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException();
        String source = attributes.getValue(LiftVocabulary.LIFT_URI, "source");
        if (source == null) throw new IllegalArgumentException();

        if (!header.getEtymologyTypeManager().hasRangeElements(type)) {
            header.getEtymologyTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement element = header.getEtymologyTypeManager().getRangeElement(type);

        LiftEtymology etym = new LiftEtymology(element, source);
        populateWithAttribute(etym, attributes);
        parent.addEtymology(etym);
        return etym;
    }

    public LiftVariant createVariant(
        Attributes attributes,
        LiftEntry liftEntry
    ) {
        LiftVariant variant = new LiftVariant();
        populateWithAttribute(variant, attributes);
        liftEntry.addVariant(variant);
        return variant;
    }

    public LiftExample createExample(
        Attributes attributes,
        LiftSense liftSense
    ) {
        LiftExample example = new LiftExample();
        populateWithAttribute(example, attributes);
        liftSense.addExample(example);
        return example;
    }

    public LiftRelation createRelation(
        Attributes attributes,
        HasRelations parent
    ) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException(
            "A relation element must have a type attribute"
        );

        if (!header.getRelationTypeManager().hasRangeElements(type)) {
            header.getRelationTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement element = header.getRelationTypeManager().getRangeElement(type);

        LiftRelation relation = new LiftRelation(element);
        populateWithAttribute(relation, attributes);
        parent.addRelation(relation);
        return relation;
    }

    public LiftPronunciation createPronounciation(
        Attributes attributes,
        HasPronunciation parent
    ) {
        LiftPronunciation pronunciation = new LiftPronunciation();
        populateWithAttribute(pronunciation, attributes);
        parent.addPronunciation(pronunciation);
        return pronunciation;
    }

    /**
     * Create and attach an empty pronunciation (editing use-case).
     */
    public LiftPronunciation createPronunciation(HasPronunciation parent) {
        LiftPronunciation pronunciation = new LiftPronunciation();
        parent.addPronunciation(pronunciation);
        return pronunciation;
    }

    public LiftField createField(
        Attributes attributes,
        AbstractExtensibleWithField parent
    ) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException(
            "Attribute type on field element cannot be null"
        );
        LiftFieldAndTraitDefinition def = header.getOrCreateFieldDefinitions(type);
        LiftField f = new LiftField(def);
        // populateWithAttribute(f, attributes);
        parent.addField(f);
        return f;
    }

    public LiftHeaderRangeElement getTranslationType(String type) {
        if (!header.getTranslationTypeManager().hasRangeElements(type)) {
            header.getTranslationTypeManager().createRangeElement(type);
        }
        return header.getTranslationTypeManager().getRangeElement(type);
    }

    public LiftTrait createTrait(Attributes attributes, HasTrait parent) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        String value = attributes.getValue(LiftVocabulary.LIFT_URI, "value");

        LiftFieldAndTraitDefinition def = header.getOrCreateTraitsDefinitions(name);

        LiftTrait trait = switch (def.getDefinitionType().get()) {
            case INTEGER -> {
                Integer v = Integer.valueOf(value);
                yield new LiftTrait(def, v);
            }
            case DATETIME -> {
                ZonedDateTime instant = ZonedDateTime.parse(value);
                yield new LiftTrait(def, instant);
            }
            case STRING -> new LiftTrait(def, value);
            case OPTION -> {
                LiftHeaderRange r = def.getResolvedRange().get();
                LiftHeaderRangeElement e = r.getOrCreateRangeElement(value);
                yield new LiftTrait(def, e);
            }
            case OPTION_COLLECTION -> {
                List<LiftHeaderRangeElement> elements = parseRangeElement(def, value);
                yield new LiftTrait(def, new HashSet(elements));
            }
            case OPTION_SEQUENCE ->{
                List<LiftHeaderRangeElement> elements = parseRangeElement(def, value);
                yield new LiftTrait(def, elements);
            }
            default -> throw new IllegalArgumentException("Unknown definition type: " + def.getDefinitionType().get());
        };

        parent.addTrait(trait);
        return trait;
    }

    private List<LiftHeaderRangeElement> parseRangeElement(LiftFieldAndTraitDefinition def, String list) {
        LiftHeaderRange r = def.getResolvedRange().get();
        List<LiftHeaderRangeElement> values = new ArrayList<>();
        for (String v : list.trim().split("\\s+")) {
            LiftHeaderRangeElement e = r.getOrCreateRangeElement(v);
            values.add(e);
        }
        return values;
    }

    public LiftNote createNoteWithAttributes(Attributes attributes, AbstractNotable parent) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        LiftNote n = createNote(type, parent);
        populateWithAttribute(n, attributes);
        return n;
    }

    public LiftNote createNote(String type, AbstractNotable parent) {
        if (type == null) type = "";
        if (!header.getNoteTypeManager().hasRangeElements(type)) {
            header.getNoteTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement element = header.getNoteTypeManager().getRangeElement(type);
        LiftNote n = new LiftNote(element);
        parent.addNote(n);
        return n;
    }

    // Not a subclass of AbstractExtensibleWithoutField.
    public LiftMedia createMedia(
        Attributes attributes,
        LiftPronunciation pronunciation
    ) {
        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        LiftMedia m = new LiftMedia(href); // mandatory
        pronunciation.addMedia(m);
        return m;
    }

    // annotation is not a subclass of the AbstractX hierarchy and cannot benefit from populate...
    public LiftAnnotation createAnnotation(
        Attributes attributes,
        HasAnnotation parent
    ) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        if (name == null) throw new IllegalArgumentException(
            "Attribute name on annotation element cannot be null"
        );
        LiftAnnotation a = new LiftAnnotation(name);

        String value = attributes.getValue(LiftVocabulary.LIFT_URI, "value");
        if (value != null) a.setValue(value);
        String who = attributes.getValue(LiftVocabulary.LIFT_URI, "who");
        if (who != null) a.setWho(who);
        String when = attributes.getValue(LiftVocabulary.LIFT_URI, "when");
        if (when != null) a.setWhen(when);

        parent.addAnnotation(a);
        return a;
    }

    private void populateWithAttribute(
        AbstractExtensibleWithoutField liftObject,
        Attributes attributes
    ) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            if (name.equals("id")) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setId(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("guid")) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setGuid(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("type")) {
                if (liftObject instanceof LiftEtymology le) {
                    // } else if (liftObject instanceof LiftField lf) {
                } else if (liftObject instanceof LiftRelation lr) {
                } else if (liftObject instanceof LiftNote ln) {
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("source")) {
                if (liftObject instanceof LiftEtymology le) {
                } else if (liftObject instanceof LiftExample le) {
                    le.setSource(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("refid")) {
                if (liftObject instanceof LiftVariant lv) {
                    registry.refId2Occurrences
                        .computeIfAbsent(value, k -> new ArrayList<>())
                        .add(lv);
                } else if (liftObject instanceof LiftRelation lr) {
                    registry.refId2Occurrences
                        .computeIfAbsent(value, k -> new ArrayList<>())
                        .add(lr);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("dateDeleted")) {
                // TODO use LiftVocabulary.DATE_DELETED_ATTRIBUTE
                if (liftObject instanceof LiftEntry le) {
                    le.setDateDeleted(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("order")) {
                if (liftObject instanceof LiftSense ls) {
                    ls.setOrder(Integer.parseInt(value));
                } else if (liftObject instanceof LiftRelation lr) {
                    lr.setOrder(Integer.parseInt(value));
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals("dateCreated")) {
                liftObject.setDateCreated(value);
            } else if (name.equals("dateModified")) {
                liftObject.setDateModified(value);
            } else {
                liftObject.getOtherXmlAttributes().put(name, value);
            }
        }
    }

    public LiftHeaderRange createRange(
        Attributes attributes,
        LiftHeader parent
    ) {
        String id = attributes.getValue(LiftVocabulary.LIFT_URI, "id");
        if (id == null) throw new IllegalArgumentException(
            "Range ID cannot be null"
        );
        LiftHeaderRange hr = new LiftHeaderRange(id, parent);

        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        if (href != null) hr.setHref(href);
        String guid = attributes.getValue(LiftVocabulary.LIFT_URI, "guid");
        if (guid != null) hr.setGuid(guid);

        parent.addRanges(hr);
        return hr;
    }

    public LiftFieldAndTraitDefinition createFieldOrTraitDefinition(
        Attributes attributes,
        LiftHeader parent
    ) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        if (name == null) name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "tag"
        );
        if (name == null) name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "guid"
        );
        if (name == null) throw new IllegalArgumentException(
            "An attribute 'name', 'tag', or 'guid' is required on field-definition"
        );
        LiftFieldAndTraitDefinition f = header.createUnknownDefinition(name);

        String fieldclass = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "class"
        );
        if (fieldclass != null) f.setTargets(fieldclass);

        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type != null) f.setType(Optional.of(type));

        String optionRange = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "option-range"
        );

        // wait until the end of the header to safely point from the FieldAndTraitDefinition to the Range object.
        if (optionRange != null) rangeId2TraitDefinitionForDereferencing.computeIfAbsent(optionRange, k -> new ArrayList<>()).add(f);

        String writingSystem = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "writing-system"
        );
        if (writingSystem != null) f.setWritingSystem(
            Optional.of(writingSystem)
        );

        return f;
    }

    public LiftHeaderRangeElement createRangeElement(
        Attributes attributes,
        LiftHeaderRange parent
    ) {
        String id = attributes.getValue(LiftVocabulary.LIFT_URI, "id");
        if (id == null) throw new IllegalArgumentException();
        // the range-element may have already been created by a reference from another range-element (see parentElementId below)
        // so we use getOrCreateRangeElement rather that createRangeElement
        LiftHeaderRangeElement hre = parent.getOrCreateRangeElement(id);

        String parentElementId = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            "parent"
        );
        if (parentElementId != null) {
            LiftHeaderRangeElement parentElement = parent.getOrCreateRangeElement(parentElementId);
            hre.setParentElement(parentElement);
        }
        String guid = attributes.getValue(LiftVocabulary.LIFT_URI, "guid");
        if (guid != null) hre.setGuid(guid);

        return hre;
    }

    public LiftAnnotation createAnnotation(String name, HasAnnotation parent) {
        LiftAnnotation a = new LiftAnnotation(name);
        parent.addAnnotation(a);
        return a;
    }

    public LiftIllustration createIllustration(
        Attributes attributes,
        LiftSense parent
    ) {
        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        if (href == null) throw new IllegalArgumentException();
        LiftIllustration ill = new LiftIllustration(href);
        parent.addIllustration(ill);
        return ill;
    }

    public LiftHeader getHeader() {
        return this.header;
    }

    public TextSpan createTextSpan() {
        return new TextSpan();
    }

    public Form createText(String lang) {
        return new Form(lang);
    }

    public void endHeader() {
        dereferenceOptionRangeInFieldAndTraitDefinitions();
    }

    public Map<String, List<LiftFieldAndTraitDefinition>> rangeId2TraitDefinitionForDereferencing = new HashMap<>();

    private void dereferenceOptionRangeInFieldAndTraitDefinitions() {
        // For each Trait Definition that register a Range,
        // add a reference to the range object to the trait definition object.
        for (String rangeId : rangeId2TraitDefinitionForDereferencing.keySet()) {
            LiftHeaderRange r = header.getRange(rangeId);
            for (LiftFieldAndTraitDefinition def : rangeId2TraitDefinitionForDereferencing.get(rangeId)) {
                def.setResolvedRange(Optional.of(r));
            }
        }
    }

    public void endDocument() {
        dereferenceHasRefTargets();
    }

	private void dereferenceHasRefTargets() {
	    for (String targetId : registry.refId2Occurrences.keySet()) {
            AbstractIdentifiable target = registry.getEntryOrSenseByLiftId(targetId);
            if (target == null) {
                throw new IllegalArgumentException(
                    "Reference id " + targetId + " not found in entries or senses."
                );
            }
            for (HasRefId source : registry.refId2Occurrences.get(targetId)) {
                source.setRefObject(target);
            }
        }
	}

	public void setGrammaticalInfo(LiftSense s, String value) {
	  if (value == null) throw new IllegalArgumentException("Grammatical info code cannot be null");
	  LiftHeaderRangeElement gramInfo = header.getGrammaticalInfoManager().getOrCreateRangeElement(value);
	  s.setGrammaticalInfo(gramInfo);
	}
}
