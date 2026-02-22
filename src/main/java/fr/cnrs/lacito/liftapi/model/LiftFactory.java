package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.xml.sax.Attributes;
import fr.cnrs.lacito.liftapi.LiftDictionaryCompoments;
import fr.cnrs.lacito.liftapi.xml.LiftVocabulary;
import lombok.Getter;
import lombok.Setter;

public final class LiftFactory implements LiftDictionaryCompoments {

    @Getter @Setter private String liftVersion;
    @Getter @Setter private String liftProducer;

    protected LiftHeader header;

    protected List<LiftEntry> allEntries = new ArrayList<>(500);
    protected Map<String, LiftEntry> entryById = new HashMap<>(500);
    protected List<LiftEntry> entryWithoutId = new ArrayList<>(200);

    protected List<String> refId = new ArrayList<>(200);

    private final List<LiftSense> allSenses = new ArrayList<>(200);
    protected Map<String, LiftSense> senseById = new HashMap<>(800);
    protected List<LiftSense> senseWithoutId = new ArrayList<>(300);

    private final List<LiftAnnotation> allAnnotations = new ArrayList<>(200);
    private final List<LiftNote> allNotes = new ArrayList<>(200);
    private final List<LiftPronunciation> allPronunciations = new ArrayList<>(200);
    private final List<LiftField> allFields = new ArrayList<>(200);
    private final List<LiftTrait> allTraits = new ArrayList<>(200);
    private final List<MultiText> allObjectLanguagesMultiText = new ArrayList<>(200);
    private final List<MultiText> allMetaLanguagesMultiText = new ArrayList<>(200);
    private final List<LiftRelation> allRelations = new ArrayList<>(200);
    private final List<LiftExample> allExamples = new ArrayList<>(200);
    private final List<LiftVariant> allVariants = new ArrayList<>(200);
    private final List<LiftMedia> allMedias = new ArrayList<>(200);
    private final List<LiftIllustration> allIllustrations = new ArrayList<>(200);
    
    // TODO to be completed for all types

    public LiftFactory() {
    }


    public List<MultiText> getAllObjectMultiText () {
        return allObjectLanguagesMultiText;
    }

    public LiftHeader createHeader() {
        this.header = new LiftHeader();
        return this.header;
    }

    public LiftEntry createEntry(Attributes attributes) {
        LiftEntry entry = new LiftEntry();
        populateWithAttribute(entry, attributes);
        Optional<String> id = entry.getId();
        if (!id.isEmpty()) {
            if (entryById.containsKey(id.get())) throw new DuplicateIdException("Duplicate id in entries: " + id);
            entryById.put(id.get(), entry);
        } else {
            entryWithoutId.add(entry);
        }

        this.allEntries.add(entry);
        this.allObjectLanguagesMultiText.add(entry.getForms());
        this.allMetaLanguagesMultiText.add(entry.getCitations());
        return entry;
    }

    public void finalize() {
        for (LiftEntry e : entryWithoutId) {
            String id = generate_id();
            e.id = Optional.of(id);
            entryById.put(id, e);
        }
        for (LiftSense s : senseWithoutId) {
            String id = generate_id();
            s.id = Optional.of(id);
            senseById.put(id, s);
        }
        for (String id : refId) {
            if (!entryById.containsKey(id)) {
                throw new IllegalArgumentException("Reference id " + id + " not found in entries");
            }
        }
    }

    private String generate_id() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generate_id'");
    }

    public LiftSense createSense(Attributes attributes, LiftSense s) {
        LiftSense sense = createSense(attributes);
        s.addSense(sense);
        return sense;
    }

    public LiftSense createSense(Attributes attributes, LiftEntry e) {
        LiftSense sense = createSense(attributes);
        e.addSense(sense);
        return sense;
    }

    private LiftSense createSense(Attributes attributes) {
        LiftSense sense = new LiftSense();
        populateWithAttribute(sense, attributes);        
        Optional<String> id = sense.getId();
        if (!id.isEmpty()) {
            senseById.put(id.get(), sense);
        } else {
            senseWithoutId.add(sense);
        }
        this.allSenses.add(sense);
        this.allMetaLanguagesMultiText.add(sense.getDefinition());
        return sense;
    }

    public LiftReversal createReversal(Attributes attributes, LiftSense sense) {
        LiftReversal reversal = new LiftReversal();
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type != null) reversal.setType(type);
        sense.addReversal(reversal);
        this.allMetaLanguagesMultiText.add(reversal.getForms());
        return reversal;
    }

    public LiftReversal createReversalMain(LiftReversal parent) {
        LiftReversal main = new LiftReversal();
        parent.setMain(main);
        this.allMetaLanguagesMultiText.add(main.getForms());
        return main;
    }

    public LiftEtymology createEtymology(Attributes attributes, LiftEntry parent) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException();
        String source = attributes.getValue(LiftVocabulary.LIFT_URI, "source");
        if (source == null) throw new IllegalArgumentException();

        LiftEtymology etym = new LiftEtymology(type, source);
        populateWithAttribute(etym, attributes);
        parent.addEtymology(etym);
        return etym;
    }

    public LiftVariant createVariant(Attributes attributes, LiftEntry liftEntry) {
        LiftVariant variant = new LiftVariant();
        populateWithAttribute(variant, attributes);
        liftEntry.addVariant(variant);
        this.allVariants.add(variant);
        this.allObjectLanguagesMultiText.add(variant.getForms());
        return variant;
    }

    public LiftExample createExample(Attributes attributes, LiftSense liftSense) {
        LiftExample example = new LiftExample();
        populateWithAttribute(example, attributes);
        liftSense.addExample(example);
        this.allExamples.add(example);
        allObjectLanguagesMultiText.add(example.getExample());
        return example;
    }

    public LiftRelation createRelation(Attributes attributes, HasRelations parent) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException("A relation element must have a type attribute");

        LiftRelation relation = new LiftRelation(type);
        populateWithAttribute(relation, attributes);
        parent.addRelation(relation);
        this.allRelations.add(relation);
        this.allMetaLanguagesMultiText.add(relation.getUsage());
        return relation;
    }

    public LiftPronunciation createPronounciation(Attributes attributes, HasPronunciation parent) {
        LiftPronunciation pronunciation = new LiftPronunciation();
        populateWithAttribute(pronunciation, attributes);
        parent.addPronunciation(pronunciation);
        this.allPronunciations.add(pronunciation);
        this.allObjectLanguagesMultiText.add(pronunciation.getProunciation());
        return pronunciation;
    }

    /**
     * Create and attach an empty pronunciation (editing use-case).
     */
    public LiftPronunciation createPronunciation(HasPronunciation parent) {
        LiftPronunciation pronunciation = new LiftPronunciation();
        parent.addPronunciation(pronunciation);
        this.allPronunciations.add(pronunciation);
        this.allObjectLanguagesMultiText.add(pronunciation.getProunciation());
        return pronunciation;
    }

    public LiftField createField(Attributes attributes, AbstractExtensibleWithField parent) {
        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type == null) throw new IllegalArgumentException("Attribute type on field element cannot be null");
        LiftField f = new LiftField(type);
        // populateWithAttribute(f, attributes);
        parent.addField(f);
        this.allFields.add(f);
        this.allMetaLanguagesMultiText.add(f.getText());
        return f;
    }

    public LiftTrait createTrait(Attributes attributes, HasTrait parent) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        String value = attributes.getValue(LiftVocabulary.LIFT_URI, "value");
        LiftTrait trait = new LiftTrait(name, value);
        parent.addTrait(trait);
        this.allTraits.add(trait);
        return trait;
    }

    /**
     * Create and attach a new trait (editing use-case).
     * This does NOT perform any duplicate checks; callers should remove/replace as needed.
     */
    public LiftTrait createTrait(String name, String value, HasTrait parent) {
        if (name == null) throw new IllegalArgumentException("Trait name cannot be null");
        if (value == null) value = "";
        LiftTrait trait = new LiftTrait(name, value);
        parent.addTrait(trait);
        this.allTraits.add(trait);
        return trait;
    }
        
    public LiftNote create_note(Attributes attributes, AbstractNotable parent) {
        LiftNote n = new LiftNote();
        populateWithAttribute(n, attributes);
        parent.addNote(n);
        this.allNotes.add(n);
        this.allMetaLanguagesMultiText.add(n.getText());
        return n;
    }

    // Not a subclass of AbstractExtensibleWithoutField.
    public LiftMedia createMedia(Attributes attributes, LiftPronunciation pronunciation) {
        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        LiftMedia m = new LiftMedia(href); // mandatory
        pronunciation.addMedia(m);
        this.allMedias.add(m);
        return m;
    }

    // annotation is not a subclass of the AbstractX hierarchy and cannot benefit from populate...
    public LiftAnnotation createAnnotation(Attributes attributes, HasAnnotation parent) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        if (name == null) throw new IllegalArgumentException("Attribute name on annotation element cannot be null");
        LiftAnnotation a = new LiftAnnotation(name);

        String value = attributes.getValue(LiftVocabulary.LIFT_URI, "value");
        if (value != null) a.setValue(value);
        String who = attributes.getValue(LiftVocabulary.LIFT_URI, "who");
        if (who != null) a.setWho(who);
        String when = attributes.getValue(LiftVocabulary.LIFT_URI, "when");
        if (when != null) a.setWhen(when);

        parent.addAnnotation(a);
        this.allAnnotations.add(a);
        this.allMetaLanguagesMultiText.add(a.getText());
        return a;
    }

    private void populateWithAttribute(AbstractExtensibleWithoutField liftObject, Attributes attributes) {
        for (int i = 0 ; i < attributes.getLength() ; i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            if (name.equals("id")) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setId(value);
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("guid")) {
                if (liftObject instanceof AbstractIdentifiable ai) {
                    ai.setGuid(value);
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("type")) {
                if (liftObject instanceof LiftEtymology le) {
                // } else if (liftObject instanceof LiftField lf) {
                } else if (liftObject instanceof LiftRelation lr) {
                } else if (liftObject instanceof LiftNote ln) {
                    ln.setType(value);
                } else {
                liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("source")) {
                if (liftObject instanceof LiftEtymology le) {
                } else if (liftObject instanceof LiftExample le) {
                    le.setSource(value);
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("refid")) {
                refId.add(value);
                if (liftObject instanceof LiftVariant lv) {
                    lv.setRefId(value);
                } else if (liftObject instanceof LiftRelation lr) {
                    lr.setRefId(value);
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("dateDeleted")) { // TODO use LiftVocabulary.DATE_DELETED_ATTRIBUTE
                if (liftObject instanceof LiftEntry le) {
                    le.setDateDeleted(value);
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("order")) {
                if (liftObject instanceof LiftSense ls) {
                    ls.setOrder(Integer.parseInt(value));
                } else if (liftObject instanceof LiftRelation lr) {
                    lr.setOrder(Integer.parseInt(value));
                } else {
                    liftObject.otherXmlAttributes.put(name, value);
                }
            } else if (name.equals("dateCreated")) {
                liftObject.setDateCreated(value);
            } else if (name.equals("dateModified")) {
                liftObject.setDateModified(value);
            } else {
                liftObject.otherXmlAttributes.put(name, value);
            }
        }
    }
    
    public List<LiftSense> getAllSenses() {
        return this.allSenses;
    }

    public LiftHeaderRange create_range(Attributes attributes, LiftHeader parent) {
        String id = attributes.getValue(LiftVocabulary.LIFT_URI, "id");
        if (id == null) throw new IllegalArgumentException("Range ID cannot be null");
        LiftHeaderRange hr = new LiftHeaderRange(id, parent);

        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        if (href != null) hr.setHref(href);
        String guid = attributes.getValue(LiftVocabulary.LIFT_URI, "guid");
        if (guid != null) hr.setGuid(guid);

        parent.getRanges().add(hr);
        return hr;
    }

    public LiftHeaderFieldDefinition create_field_definition(Attributes attributes, LiftHeader parent) {
        String name = attributes.getValue(LiftVocabulary.LIFT_URI, "name");
        if (name == null) throw new IllegalArgumentException("An attribute 'name' is required on field definition");
        LiftHeaderFieldDefinition f = new LiftHeaderFieldDefinition(name, parent);

        String fieldclass = attributes.getValue(LiftVocabulary.LIFT_URI, "class");
        if (fieldclass != null) f.setFClass(Optional.of(fieldclass));

        String type = attributes.getValue(LiftVocabulary.LIFT_URI, "type");
        if (type != null) f.setType(Optional.of(type));

        String optionRange = attributes.getValue(LiftVocabulary.LIFT_URI, "option-range");
        if (optionRange != null) f.setOptionRange(Optional.of(optionRange));

        String writingSystem = attributes.getValue(LiftVocabulary.LIFT_URI, "writing-system");
        if (writingSystem != null) f.setWritingSystem(Optional.of(writingSystem));
        
        parent.getFields().add(f);
        return f;
    }

    public LiftHeaderRangeElement create_range_element(Attributes attributes, LiftHeaderRange parent) {
        String id = attributes.getValue(LiftVocabulary.LIFT_URI, "id");
        if (id == null) throw new IllegalArgumentException();
        LiftHeaderRangeElement hre = new LiftHeaderRangeElement(id, parent);

        String otherparent = attributes.getValue(LiftVocabulary.LIFT_URI, "parent");
        if (otherparent != null) hre.setOtherParent(otherparent);
        String guid = attributes.getValue(LiftVocabulary.LIFT_URI, "guid");
        if (guid != null) hre.setGuid(guid);

        parent.getRangeElements().add(hre);
        return hre;
    }

    public LiftHeaderRange createRange(String id, LiftHeader parent) {
        LiftHeaderRange hr = new LiftHeaderRange(id, parent);
        parent.getRanges().add(hr);
        return hr;
    }

    public LiftHeaderRangeElement createRangeElement(String id, LiftHeaderRange parent) {
        LiftHeaderRangeElement hre = new LiftHeaderRangeElement(id, parent);
        parent.getRangeElements().add(hre);
        return hre;
    }

    /**
     * Post-process field-definitions with UNKNOWN kind by checking whether
     * the name matches a trait name or a field name actually used in the dictionary.
     * A name that appears as a trait name is classified as TRAIT; as a field name, FIELD.
     */
    public void resolveFieldDefinitionKinds() {
        if (header == null) return;
        java.util.Set<String> traitNames = allTraits.stream().map(LiftTrait::getName).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> fieldNames = allFields.stream().map(LiftField::getName).collect(java.util.stream.Collectors.toSet());

        for (LiftHeaderFieldDefinition fd : header.getFields()) {
            if (fd.getKind() != FieldDefinitionKind.UNKNOWN) continue;
            if (traitNames.contains(fd.getName())) {
                fd.setKind(FieldDefinitionKind.TRAIT);
            } else if (fieldNames.contains(fd.getName())) {
                fd.setKind(FieldDefinitionKind.FIELD);
            }
        }
    }

    public LiftHeaderFieldDefinition createFieldDefinition(String name, LiftHeader parent) {
        LiftHeaderFieldDefinition fd = new LiftHeaderFieldDefinition(name, parent);
        parent.getFields().add(fd);
        return fd;
    }

    public LiftNote createNote(String type, AbstractNotable parent) {
        LiftNote n = new LiftNote();
        if (type != null) n.setType(type);
        parent.addNote(n);
        this.allNotes.add(n);
        this.allMetaLanguagesMultiText.add(n.getText());
        return n;
    }

    public LiftField createField(String type, AbstractExtensibleWithField parent) {
        LiftField f = new LiftField(type);
        parent.addField(f);
        this.allFields.add(f);
        this.allMetaLanguagesMultiText.add(f.getText());
        return f;
    }

    public LiftAnnotation createAnnotation(String name, HasAnnotation parent) {
        LiftAnnotation a = new LiftAnnotation(name);
        parent.addAnnotation(a);
        this.allAnnotations.add(a);
        this.allMetaLanguagesMultiText.add(a.getText());
        return a;
    }

    public LiftIllustration create_illustration(Attributes attributes, LiftSense parent) {
        String href = attributes.getValue(LiftVocabulary.LIFT_URI, "href");
        if (href == null) throw new IllegalArgumentException();
        LiftIllustration ill = new LiftIllustration(href);
        parent.addIllustration(ill);
        allIllustrations.add(ill);
        allMetaLanguagesMultiText.add(ill.getLabel());
        return ill;
    }

    public LiftDictionaryCompoments getLiftDictionaryCompoments() {
        return (LiftDictionaryCompoments)this;
    }

    @Override
    public List<LiftEntry> getAllEntries() {
        return allEntries;
    }

    @Override
    public Map<String, LiftEntry> getEntryById() {
        return entryById;
    }

    @Override
    public List<LiftEntry> getEntryWithoutId() {
        return entryWithoutId;
    }

    @Override
    public Map<String, LiftSense> getSenseById() {
        return senseById;
    }

    @Override
    public List<LiftSense> getSenseWithoutId() {
        return senseWithoutId;
    }

    @Override
    public List<LiftAnnotation> getAllAnnotations() {
        return allAnnotations;
    }

    @Override
    public List<LiftNote> getAllNotes() {
        return allNotes;
    }

    @Override
    public List<LiftPronunciation> getAllPronunciations() {
        return allPronunciations;
    }

    @Override
    public List<LiftField> getAllFields() {
        return allFields;
    }

    @Override
    public List<LiftTrait> getAllTraits() {
        return allTraits;
    }

    @Override
    public List<MultiText> getAllObjectLanguagesMultiText() {
        return allObjectLanguagesMultiText;
    }

    @Override
    public List<MultiText> getAllMetaLanguagesMultiText() {
        return allMetaLanguagesMultiText;
    }

    @Override
    public List<LiftRelation> getAllRelations() {
        return allRelations;
    }

    @Override
    public List<LiftExample> getAllExamples() {
        return allExamples;
    }

    @Override
    public List<LiftVariant> getAllVariants() {
        return allVariants;
    }

    @Override
    public List<LiftMedia> getAllMedias() {
        return allMedias;
    }

    @Override
    public List<LiftIllustration> getAllIllustrations() {
        return allIllustrations;
    }

    @Override
    public LiftHeader getHeader() {
        return this.header;
    }


    public TextSpan createTextSpan() {
        return new TextSpan();
    }


    public Form createText(String lang) {
        return new Form(lang);
    }

}
