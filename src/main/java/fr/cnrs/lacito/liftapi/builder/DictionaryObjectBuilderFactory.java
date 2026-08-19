package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryBuilder;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

/**
 * Static factory for creating builder instances using fluent API,
 * ensuring that all created objects are consistently registered in
 * a dictionary.
 *
 * Usage:
 * <pre>
 *  Builder builder = dictionary.getComponentBuilder();
 *  builder.entry()
 *      .withId("entry1")
 *      .withForm("en", "entry1")
 *      .build();
 * </pre>
 *
 * @see LiftDictionaryRegistry
 * @see LiftDitionary
 * @see LiftDictionaryBuilder
 */
public class DictionaryObjectBuilderFactory {

    private final LiftDictionaryRegistry registry;
    private final LiftDictionary dictionary;

    public DictionaryObjectBuilderFactory(LiftDictionary dictionary) {
        this.dictionary = dictionary;
        this.registry = dictionary.getLiftDictionaryRegistry();
    }

    /**
     * Create a new entry builder.
     */
    public EntryBuilder entry() {
        return new EntryBuilder(dictionary, null);
    }

    /**
     * Create a quick entry with a single form and gloss.
     */
    public LiftEntry entry(
        String objectLang,
        String form,
        String metaLang,
        String gloss
    ) {
        return entry()
            .withForm(objectLang, form)
            .addSense(s -> s.withGloss(metaLang, gloss))
            .build();
    }

    /**
     * Create a quick entry with form, gloss, and definition.
     */
    public LiftEntry entry(
        String objectLang,
        String form,
        String metaLang,
        String gloss,
        String definition
    ) {
        return entry()
            .withForm(objectLang, form)
            .addSense(s ->
                s
                    .withGloss(metaLang, gloss)
                    .withDefinition(metaLang, definition)
            )
            .build();
    }

    /**
     * Create a new sense builder.
     */
    public SenseBuilder sense(HasSense parent) {
        return new SenseBuilder(dictionary, parent);
    }

    /**
     * Create a quick sense with gloss and definition.
     */
    public LiftSense sense(HasSense parent, String lang, String gloss, String definition) {
        return sense(parent)
            .withGloss(lang, gloss)
            .withDefinition(lang, definition)
            .build();
    }

    /**
     * Create a new variant builder.
     */
    public VariantBuilder variant(LiftEntry entry) {
        return new VariantBuilder(dictionary, entry);
    }

    /**
     * Create a quick variant with a single form.
     */
    public LiftVariant variant(LiftEntry entry, String lang, String form) {
        return variant(entry).withForm(lang, form).build();
    }

    /**
     * Create a new pronunciation builder.
     */
    public PronunciationBuilder pronunciation(HasPronunciation parent) {
        return new PronunciationBuilder(dictionary, parent);
    }

    /**
     * Create a quick pronunciation with a single language representation.
     */
    public LiftPronunciation pronunciation(
        HasPronunciation parent,
        String language,
        String phoneticForm
    ) {
        return pronunciation(parent)
            .withPronunciation(language, phoneticForm)
            .build();
    }

    /**
     * Create a new example builder.
     */
    public ExampleBuilder example(LiftSense parent) {
        return new ExampleBuilder(dictionary, parent);
    }

    /**
     * Create a quick example with example text.
     */
    public LiftExample example(LiftSense parent, String language, String exampleText) {
        return example(parent).withExample(language, exampleText).build();
    }

    /**
     * Create a new note builder.
     */
    public NoteBuilder note(HasNote parent, String type) {
        return new NoteBuilder(dictionary, parent, type);
    }

    /**
     * Create a new field builder.
     */
    public FieldBuilder field(HasField parent,String name) {
        return new FieldBuilder(dictionary, parent, name);
    }

    /**
     * Create a new trait builder.
     */
    public TraitBuilder trait(HasTrait parent,String name, String value) {
        return new TraitBuilder(dictionary, parent, name, value);
    }

//    /**
//     * Create a new multi-text builder.
//     */
//    public MultitextObjectLanguageBuilder multiText() {
//        return new MultitextObjectLanguageBuilder(registry);
//    }
//
//    public MultitextMetaLanguageBuilder multiTextMetaLanguage() {
//        return new MultitextMetaLanguageBuilder(registry);
//    }

    /**
     * Create a new annotation builder.
     */
    public AnnotationBuilder annotation(HasAnnotation parent, String name) {
        return new AnnotationBuilder(dictionary, parent, name);
    }

    /**
     * Create a new annotation builder.
     */
    public AnnotationBuilder annotation(HasAnnotation parent) {
        return new AnnotationBuilder(dictionary, parent);
    }

    /**
     * Create a quick annotation with name and value.
     */
    public LiftAnnotation annotation(HasAnnotation parent, String name, String value) {
        return new AnnotationBuilder(dictionary, parent, name).withValue(value).build();
    }

    /**
     * Create a new relation builder with the given type.
     */
    public RelationBuilder relation(String type, HasRelations parent) {
        return new RelationBuilder(dictionary, parent, type);
    }

    /**
     * Create a quick relation with type and target ID.
     */
    public LiftRelation relation(HasRelations parent, String type, String targetId) {
        return relation(type, parent).withRefId(targetId).build();
    }

    /**
     * Create a new etymology builder.
     */
    public EtymologyBuilder etymology(LiftEntry parent, String type, String source) {
        return new EtymologyBuilder(dictionary, parent, type, source);
    }

    public LiftHeaderRangeBuilder range(String rangeId) {
        return new LiftHeaderRangeBuilder(dictionary, rangeId);
    }
}
