package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;

/**
 * Static factory for creating builder instances using fluent API.
 * 
 * Usage:
 * <pre>
 *   LiftEntry entry = Builders.entry()
 *       .withForm("en", "dictionary")
 *       .addSense(s -> s
 *           .withGloss("en", "reference book")
 *           .withDefinition("en", "A book of words and definitions")
 *       )
 *       .build();
 * </pre>
 */
public class Builders {
    
    private Builders() {
        // Prevent instantiation
    }

    /**
     * Create a new entry builder.
     */
    public static DictionaryBuilder dictionary() {
        return new DictionaryBuilder();
    }

    /**
     * Create a new entry builder.
     */
    public static EntryBuilder entry() {
        return new EntryBuilder();
    }

    /**
     * Create a quick entry with a single form and gloss.
     */
    public static LiftEntry entry(String form, String gloss) {
        return entry()
            .withForm("en", form)
            .addSense(s -> s.withGloss("en", gloss))
            .build();
    }

    /**
     * Create a quick entry with form, gloss, and definition.
     */
    public static LiftEntry entry(String form, String gloss, String definition) {
        return entry()
            .withForm("en", form)
            .addSense(s -> s
                .withGloss("en", gloss)
                .withDefinition("en", definition)
            )
            .build();
    }

    /**
     * Create a new sense builder.
     */
    public static SenseBuilder sense() {
        return new SenseBuilder();
    }

    /**
     * Create a quick sense with gloss and definition.
     */
    public static LiftSense sense(String gloss, String definition) {
        return sense()
            .withGloss("en", gloss)
            .withDefinition("en", definition)
            .build();
    }

    /**
     * Create a new variant builder.
     */
    public static VariantBuilder variant() {
        return new VariantBuilder();
    }

    /**
     * Create a quick variant with a single form.
     */
    public static LiftVariant variant(String form) {
        return variant()
            .withForm("en", form)
            .build();
    }

    /**
     * Create a new pronunciation builder.
     */
    public static PronunciationBuilder pronunciation() {
        return new PronunciationBuilder();
    }

    /**
     * Create a quick pronunciation with a single language representation.
     */
    public static LiftPronunciation pronunciation(String language, String phoneticForm) {
        return pronunciation()
            .withPronunciation(language, phoneticForm)
            .build();
    }

    /**
     * Create a new example builder.
     */
    public static ExampleBuilder example() {
        return new ExampleBuilder();
    }

    /**
     * Create a quick example with example text.
     */
    public static LiftExample example(String language, String exampleText) {
        return example()
            .withExample(language, exampleText)
            .build();
    }

    /**
     * Create a new note builder.
     */
    public static NoteBuilder note() {
        return new NoteBuilder();
    }

    /**
     * Create a new field builder.
     */
    public static FieldBuilder field(String name) {
        return new FieldBuilder(name);
    }

    /**
     * Create a new trait builder.
     */
    public static TraitBuilder trait(String name, String value) {
        return new TraitBuilder(name, value);
    }

    /**
     * Create a new multi-text builder.
     */
    public static MultiTextBuilder multiText() {
        return new MultiTextBuilder();
    }

    /**
     * Create a new annotation builder.
     */
    public static AnnotationBuilder annotation(String name) {
        return new AnnotationBuilder(name);
    }

    /**
     * Create a new annotation builder.
     */
    public static AnnotationBuilder annotation() {
        return new AnnotationBuilder();
    }

    /**
     * Create a quick annotation with name and value.
     */
    public static LiftAnnotation annotation(String name, String value) {
        return new AnnotationBuilder(name)
            .withValue(value)
            .build();
    }

    /**
     * Create a new relation builder with the given type.
     */
    public static RelationBuilder relation(String type) {
        return new RelationBuilder(type);
    }

    /**
     * Create a quick relation with type and target ID.
     */
    public static LiftRelation relation(String type, String targetId) {
        return relation(type)
            .withRefId(targetId)
            .build();
    }

    /**
     * Create a new etymology builder.
     */
    public static EtymologyBuilder etymology(String type, String source) {
        return new EtymologyBuilder(type, source);
    }

    /**
     * Create a new etymology builder.
     */
    public static EtymologyBuilder etymology() {
        return new EtymologyBuilder();
    }
}
