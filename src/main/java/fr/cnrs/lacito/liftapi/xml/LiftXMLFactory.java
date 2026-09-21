package fr.cnrs.lacito.liftapi.xml;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryLanguagesManager;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.*;
import javafx.collections.ObservableList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;

/**
 * Factory class for creating components from LIFT XML elements and structures
 * using a lower-level API than the builder API for efficiency object generation.
 */
public final class LiftXMLFactory {

    public void setLiftVersion(LiftVersion liftVersion) {
        this.dictionary.setLiftVersion(liftVersion);
    }

    public void setLiftProducer(String liftProducer) {
        this.dictionary.setLiftProducer(liftProducer);
    }

    protected LiftHeader header;
    private LiftDictionaryRegistry registry;
    private LiftDictionary dictionary;

    public LiftXMLFactory(LiftDictionary dictionary) {
        this.dictionary = dictionary;
        this.header = dictionary.getHeader();
        this.registry = dictionary.getLiftDictionaryRegistry();

        // TODO Ugly hack n°1
        this.dictionary.turnOffLanguageManager();
    }

    // TODO all these methods should be turned protected
    public LiftHeader getHeader() {
        return this.dictionary.getHeader();
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
        // reversal-content declares @type as optional.
        String type = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.TYPE_ATTRIBUTE
        );
        Feature element = null;
        if (type != null) {
            if (!header.getNoteTypeManager().hasFeature(type)) {
                header.getNoteTypeManager().addFeature(type);
            }
            element = header.getNoteTypeManager().getFeature(type);
        }
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
        String type = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.TYPE_ATTRIBUTE
        );
        if (type == null) throw new IllegalArgumentException(
            "etymology-content requires a 'type' attribute"
        );
        String source = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.SOURCE_ATTRIBUTE
        );
        if (source == null) throw new IllegalArgumentException(
            "etymology-content requires a 'source' attribute"
        );

        if (!header.getEtymologyTypeManager().hasFeature(type)) {
            header.getEtymologyTypeManager().addFeature(type);
        }
        Feature element = header.getEtymologyTypeManager().getFeature(type);

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
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type == null) throw new IllegalArgumentException(
            "A relation element must have a type attribute"
        );

        if (!header.getRelationTypeManager().hasFeature(type)) {
            header.getRelationTypeManager().addFeature(type);
        }
        Feature element = header.getRelationTypeManager().getFeature(type);

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
        // field-content names its definition with @type in LIFT 0.13 and with @name
        // in LIFT 0.15. Accept either, whatever the declared version, so that files
        // mixing the two (FLEx does) still load.
        String name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.NAME_ATTRIBUTE
        );
        if (name == null) name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.TYPE_ATTRIBUTE
        );
        if (name == null) throw new IllegalArgumentException(
            "A field element requires a 'name' (LIFT 0.15) or 'type' (LIFT 0.13) attribute"
        );
        LiftFieldAndTraitDefinition def = header.getOrCreateFieldDefinitions(name);
        LiftField f = new LiftField(def);
        // populateWithAttribute(f, attributes);
        parent.addField(f);
        return f;
    }

    public Feature getTranslationType(String type) {
        if (!header.getTranslationTypeManager().hasFeature(type)) {
            header.getTranslationTypeManager().addFeature(type);
        }
        return header.getTranslationTypeManager().getFeature(type);
    }

    public LiftTrait createTrait(Attributes attributes, HasTrait parent) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.NAME_ATTRIBUTE);
        String value = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.VALUE_ATTRIBUTE);

        LiftFieldAndTraitDefinition def = header.getOrCreateTraitsDefinitions(name);

        LiftTrait trait = switch (def.getDataModel().get()) {
            case INTEGER -> {
                Integer v = Integer.valueOf(value);
                yield new LiftTrait(def, v);
            }
            case DATETIME -> {
                ZonedDateTime instant = ZonedDateTime.parse(value);
                yield new LiftTrait(def, instant);
            }
            case STRING -> new LiftTrait(def, value);
            case FEATURE -> {
                FeatureSet r = def.getResolvedFeatureSet().get();
                Feature e = r.getOrCreateFeature(value);
                yield new LiftTrait(def, e);
            }
            case FEATURE_SET -> {
                List<Feature> elements = parseRangeElement(def, value);
                yield new LiftTrait(def, new HashSet<>(elements));
            }
            case FEATURE_LIST ->{
                List<Feature> elements = parseRangeElement(def, value);
                yield new LiftTrait(def, elements);
            }
            default -> throw new IllegalArgumentException("Unknown definition type: " + def.getDataModel().get());
        };

        parent.addTrait(trait);
        return trait;
    }

    private List<Feature> parseRangeElement(LiftFieldAndTraitDefinition def, String list) {
        FeatureSet r = def.getResolvedFeatureSet().get();
        List<Feature> values = new ArrayList<>();
        for (String v : list.trim().split("\\s+")) {
            Feature e = r.getOrCreateFeature(v);
            values.add(e);
        }
        return values;
    }

    public LiftNote createNoteWithAttributes(Attributes attributes, AbstractNotable parent) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.TYPE_ATTRIBUTE);
        LiftNote n = createNote(type, parent);
        populateWithAttribute(n, attributes);
        return n;
    }

    public LiftNote createNote(String type, AbstractNotable parent) {
        if (type == null) type = "";
        if (!header.getNoteTypeManager().hasFeature(type)) {
            header.getNoteTypeManager().addFeature(type);
        }
        Feature element = header.getNoteTypeManager().getFeature(type);
        LiftNote n = new LiftNote(element);
        parent.addNote(n);
        return n;
    }

    // Not a subclass of AbstractExtensibleWithoutField.
    public LiftMedia createMedia(
        Attributes attributes,
        LiftPronunciation pronunciation
    ) {
        String href = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.HREF_ATTRIBUTE);
        LiftMedia m = new LiftMedia(href); // mandatory
        pronunciation.addMedia(m);
        return m;
    }

    public LiftAnnotation createAnnotation(String name, HasAnnotation parent) {
        if (name == null) throw new IllegalArgumentException(
            "Attribute name on annotation element cannot be null"
        );

        if (!header.getAnnotationTypeManager().hasFeature(name)) {
            header.getAnnotationTypeManager().addFeature(name);
        }
        Feature element = header.getAnnotationTypeManager().getFeature(name);

        LiftAnnotation a = new LiftAnnotation(element);

        parent.addAnnotation(a);
        return a;
    }

    // annotation is not a subclass of the AbstractX hierarchy and cannot benefit from populate...
    public LiftAnnotation createAnnotation(
        Attributes attributes,
        HasAnnotation parent
    ) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.NAME_ATTRIBUTE);

        LiftAnnotation a = createAnnotation(name, parent);

        String value = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.VALUE_ATTRIBUTE);
        if (value != null) a.setValue(value);
        String who = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.WHO_ATTRIBUTE);
        if (who != null) a.setWho(who);
        String when = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.WHEN_ATTRIBUTE);
        if (when != null) a.setWhen(when);

        return a;
    }

    private void populateWithAttribute(
        AbstractExtensibleWithoutField liftObject,
        Attributes attributes
    ) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            if (name.equals(LiftVocabulary.ID_ATTRIBUTE)) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setId(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.GUID_ATTRIBUTE)) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setGuid(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.TYPE_ATTRIBUTE)) {
                if (liftObject instanceof LiftEtymology _) {
                    // } else if (liftObject instanceof LiftField lf) {
                } else if (liftObject instanceof LiftRelation _) {
                } else if (liftObject instanceof LiftNote _) {
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.SOURCE_ATTRIBUTE)) {
                if (liftObject instanceof LiftEtymology _) {
                } else if (liftObject instanceof LiftExample le) {
                    le.setSource(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.REF_ATTRIBUTE)) {
                if (! value.trim().isEmpty()) {
                    if (liftObject instanceof LiftVariant lv) {
                        lv.setRefId(value);
                        // done in register.register()
                        // registry.refId2HasRefIdList
                        //     .computeIfAbsent(value, k -> new ArrayList<>())
                        //     .add(lv);
                    } else if (liftObject instanceof LiftRelation lr) {
                        lr.setRefId(value);
                        // done in register.register()
                        // registry.refId2HasRefIdList
                        //     .computeIfAbsent(value, k -> new ArrayList<>())
                        //     .add(lr);
                    } else {
                        liftObject.getOtherXmlAttributes().put(name, value);
                    }
                }
            } else if (name.equals(LiftVocabulary.DATE_DELETED_ATTRIBUTE)) {
                // TODO use LiftVocabulary.DATE_DELETED_ATTRIBUTE
                if (liftObject instanceof LiftEntry le) {
                    le.setDateDeleted(value);
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.ORDER_ATTRIBUTE)) {
                if (liftObject instanceof LiftSense ls) {
                    ls.setOrder(Integer.parseInt(value));
                } else if (liftObject instanceof LiftRelation lr) {
                    lr.setOrder(Integer.parseInt(value));
                } else {
                    liftObject.getOtherXmlAttributes().put(name, value);
                }
            } else if (name.equals(LiftVocabulary.DATE_CREATED_ATTRIBUTE)) {
                liftObject.setDateCreated(value);
            } else if (name.equals(LiftVocabulary.DATE_MODIFIED_ATTRIBUTE)) {
                liftObject.setDateModified(value);
            } else {
                liftObject.getOtherXmlAttributes().put(name, value);
            }
        }
    }

    public FeatureSet createRange(
        Attributes attributes,
        LiftHeader parent
    ) {
        String id = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.ID_ATTRIBUTE
        );
        if (id == null) throw new IllegalArgumentException(
            "Range ID cannot be null"
        );
        FeatureSet hr = null;

        // TODO that should happen only for :
        // "note-type";
        // "translation-type";
        // "grammatical-info";
        // "relation-type";
        // "inverse-type";
        // "annotation-type";
        // "etymology-type";
        // "variant-type";
        // because they are automatically created
        if (parent.hasFeatureSet(id)) {
            hr = parent.getFeatureSet(id);
        } else {
            hr = new FeatureSet(id, parent);
            String href = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.HREF_ATTRIBUTE);
            if (href != null) hr.setHref(href);
            String guid = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.GUID_ATTRIBUTE);
            if (guid != null) hr.setGuid(guid);
            parent.addFeatureSet(hr);
        }
        return hr;
    }

    public LiftFieldAndTraitDefinition createFieldOrTraitDefinition(
        Attributes attributes,
        LiftHeader parent
    ) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.NAME_ATTRIBUTE);
        if (name == null) name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.TAG_ATTRIBUTE
        );
        if (name == null) name = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.GUID_ATTRIBUTE
        );
        if (name == null) throw new IllegalArgumentException(
            "An attribute 'name', 'tag', or 'guid' is required on field-definition"
        );
        LiftFieldAndTraitDefinition f = header.createUnknownDefinition(name);

        String fieldclass = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.CLASS_ATTRIBUTE
        );
        if (fieldclass != null) f.setTargets(fieldclass);

        String type = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.TYPE_ATTRIBUTE);
        if (type != null) f.setDataModel(Optional.of(type));

        String optionRange = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.OPTION_RANGE_ATTRIBUTE
        );

        // wait until the end of the header to safely point from the FieldAndTraitDefinition to the Range object.
        if (optionRange != null) rangeId2TraitDefinitionForDereferencing.computeIfAbsent(optionRange, k -> new ArrayList<>()).add(f);

        String writingSystem = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.WRITING_SYSTEM_ATTRIBUTE
        );
        if (writingSystem != null) f.setWritingSystem(
            Optional.of(writingSystem)
        );

        return f;
    }

    public Feature createRangeElement(
        Attributes attributes,
        FeatureSet parent
    ) {
        String id = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.ID_ATTRIBUTE
        );
        if (id == null) throw new IllegalArgumentException(
            "range-element requires an 'id' attribute"
        );
        // the range-element may have already been created by a reference from another range-element (see parentElementId below)
        // so we use getOrCreateRangeElement rather that createRangeElement
        // TODO however duplicate range-element ids should be detected and reported as an error
        Feature hre = parent.getOrCreateFeature(id);

        // If a feature has a parent feature,
        // The link parent -> child is stored in
        // featureSet2Feature2ChildFeature; the
        // actual reference from the child to the parent
        // is created a the end of the parsing of the header,
        // when we can be sure that the parent have been created
        String parentElementId = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.PARENT_ATTRIBUTE
        );
        if (parentElementId != null) {
            // Key by the *parent's* id: this maps parent -> children, and it must go
            // into the field, not a fresh local map that is thrown away on return.
            featureSet2Feature2ChildFeature
                .computeIfAbsent(parent.getId(), k -> new HashMap<>())
                .computeIfAbsent(parentElementId, k -> new ArrayList<>())
                .add(hre);
        }

        String guid = attributes.getValue(LiftVocabulary.LIFT_URI, LiftVocabulary.GUID_ATTRIBUTE);
        if (guid != null) hre.setGuid(guid);

        return hre;
    }


    public LiftIllustration createIllustration(
        Attributes attributes,
        LiftSense parent
    ) {
        String href = attributes.getValue(
            LiftVocabulary.LIFT_URI,
            LiftVocabulary.HREF_ATTRIBUTE
        );
        if (href == null) throw new IllegalArgumentException(
            "URLRef-content requires an 'href' attribute on illustration"
        );
        LiftIllustration ill = new LiftIllustration(href);
        parent.addIllustration(ill);
        return ill;
    }


    public TextSpan createTextSpan() {
        return new TextSpan();
    }

    public Form createText(String lang) {
        return new Form(lang);
    }

    public void endHeader() {
        dereferenceOptionRangeInFieldAndTraitDefinitions();
        dereferenceParentFeature();
    }

    /** Feature set id -&gt; parent feature id -&gt; the features declaring that parent. */
    private final Map<String, Map<String, List<Feature>>> featureSet2Feature2ChildFeature =
        new HashMap<>();

    /**
     * Resolve every {@code range-element/@parent} recorded while reading the header.
     *
     * This runs at the end of the header so that a parent declared after its children
     * is already known.
     */
    private void dereferenceParentFeature() {
        for (Map.Entry<String, Map<String, List<Feature>>> set : featureSet2Feature2ChildFeature.entrySet()) {
            FeatureSet r = header.getFeatureSet(set.getKey());
            for (Map.Entry<String, List<Feature>> byParent : set.getValue().entrySet()) {
                Feature parent = r.getOrCreateFeature(byParent.getKey());
                for (Feature f : byParent.getValue()) {
                    f.setSuperordinateFeature(parent);
                }
            }
        }
        featureSet2Feature2ChildFeature.clear();
    }
    public Map<String, List<LiftFieldAndTraitDefinition>> rangeId2TraitDefinitionForDereferencing = new HashMap<>();

    private void dereferenceOptionRangeInFieldAndTraitDefinitions() {
        // For each Trait Definition that register a Range,
        // add a reference to the range object to the trait definition object.
        for (String rangeId : rangeId2TraitDefinitionForDereferencing.keySet()) {
            FeatureSet r = header.getFeatureSet(rangeId);
            for (LiftFieldAndTraitDefinition def : rangeId2TraitDefinitionForDereferencing.get(rangeId)) {
                def.setResolvedRange(Optional.of(r));
            }
        }
    }

    protected void endDocument() {
        dereferenceHasRefTargets();

        // TODO Ugly hack n°1
        this.dictionary.turnOnLanguageManager();
        createLanguages(registry.getMetaText(), dictionary.getMetaLanguageManager());
        createLanguages(registry.getObjectText(), dictionary.getObjectLanguageManager());
    }

	private void createLanguages(ObservableList<MultiText> multiTexts, LiftDictionaryLanguagesManager languageManager) {
        // Map<String, Long> languageCounts = multiTexts
        //     .stream()
        //     .flatMap(x -> x.getForms().stream())
        //     .collect(Collectors.groupingBy(x -> x.getLang(), Collectors.counting()));;

        // for (String lang : languageCounts.keySet()) {
        //     languageManager.addLanguage(lang);
        //     languageManager.setLanguageOccurrence(lang, languageCounts.get(lang));
        // }
        Set<String> languages = multiTexts
             .stream()
             .flatMap(
                x -> x.getForms().stream()
             )
             .map(y -> y.getLang())
             .collect(Collectors.toSet());

        for (String lang : languages) {
             languageManager.addLanguage(lang);
        }
        for (MultiText m : multiTexts) {
            m.setLanguagesManager(languageManager);
        }
    }

    private void dereferenceHasRefTargets() {
	    for (String targetId : registry.refId2HasRefIdList.keySet()) {
            AbstractIdentifiable target = registry.getEntryOrSenseByLiftId(targetId);
            if (target == null) {
                throw new IllegalArgumentException(
                    "Reference id " + targetId + " not found in entries or senses."
                );
            }
            for (HasRefId source : registry.refId2HasRefIdList.get(targetId)) {
                source.setRefObject(target);
            }
        }
	}

	public void setGrammaticalInfo(LiftSense s, String value) {
	  if (value == null) throw new IllegalArgumentException("Grammatical info code cannot be null");
	  Feature gramInfo = header.getGrammaticalInfoManager().getOrCreateFeature(value);
	  s.setGrammaticalInfo(gramInfo);
	}
}
