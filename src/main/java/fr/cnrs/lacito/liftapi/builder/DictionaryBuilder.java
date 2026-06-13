package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
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
 *   LiftDictionary dictionary = Builders.dictionary()
 *       .withLiftVersion("1.0")
 *       .withProducer("MyProducer")
 *       .withDescription("en", "My Dictionary")
 *       .build();
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
public class DictionaryBuilder {

    private final LiftDictionaryRegistry registry;

    public DictionaryBuilder(LiftDictionaryRegistry registry) {
        this.registry = registry;
    }

    /**
     * Create a new entry builder.
     */
    public EntryBuilder entry() {
        return new EntryBuilder(registry);
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
    public SenseBuilder sense() {
        return new SenseBuilder(registry);
    }

    /**
     * Create a quick sense with gloss and definition.
     */
    public LiftSense sense(String lang, String gloss, String definition) {
        return sense()
            .withGloss(lang, gloss)
            .withDefinition(lang, definition)
            .build();
    }

    /**
     * Create a new variant builder.
     */
    public VariantBuilder variant() {
        return new VariantBuilder(registry);
    }

    /**
     * Create a quick variant with a single form.
     */
    public LiftVariant variant(String lang, String form) {
        return variant().withForm(lang, form).build();
    }

    /**
     * Create a new pronunciation builder.
     */
    public PronunciationBuilder pronunciation() {
        return new PronunciationBuilder(registry);
    }

    /**
     * Create a quick pronunciation with a single language representation.
     */
    public LiftPronunciation pronunciation(
        String language,
        String phoneticForm
    ) {
        return pronunciation()
            .withPronunciation(language, phoneticForm)
            .build();
    }

    /**
     * Create a new example builder.
     */
    public ExampleBuilder example() {
        return new ExampleBuilder(registry);
    }

    /**
     * Create a quick example with example text.
     */
    public LiftExample example(String language, String exampleText) {
        return example().withExample(language, exampleText).build();
    }

    /**
     * Create a new note builder.
     */
    public NoteBuilder note() {
        return new NoteBuilder(registry);
    }

    /**
     * Create a new field builder.
     */
    public FieldBuilder field(String name) {
        return new FieldBuilder(registry, name);
    }

    /**
     * Create a new trait builder.
     */
    public TraitBuilder trait(String name, String value) {
        return new TraitBuilder(registry, name, value);
    }

    /**
     * Create a new multi-text builder.
     */
    public MultitextObjectLanguageBuilder multiText() {
        return new MultitextObjectLanguageBuilder(registry);
    }

    public MultitextMetaLanguageBuilder multiTextMetaLanguage() {
        return new MultitextMetaLanguageBuilder(registry);
    }

    /**
     * Create a new annotation builder.
     */
    public AnnotationBuilder annotation(String name) {
        return new AnnotationBuilder(registry, name);
    }

    /**
     * Create a new annotation builder.
     */
    public AnnotationBuilder annotation() {
        return new AnnotationBuilder(registry);
    }

    /**
     * Create a quick annotation with name and value.
     */
    public LiftAnnotation annotation(String name, String value) {
        return new AnnotationBuilder(registry, name).withValue(value).build();
    }

    /**
     * Create a new relation builder with the given type.
     */
    public RelationBuilder relation(String type) {
        return new RelationBuilder(registry, type);
    }

    /**
     * Create a quick relation with type and target ID.
     */
    public LiftRelation relation(String type, String targetId) {
        return relation(type).withRefId(targetId).build();
    }

    /**
     * Create a new etymology builder.
     */
    public EtymologyBuilder etymology(String type, String source) {
        return new EtymologyBuilder(registry, type, source);
    }

    /**
     * Create a new etymology builder.
     */
    public EtymologyBuilder etymology() {
        return new EtymologyBuilder(registry);
    }
}
