package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.Form;

/**
 * This class is a builder for the {@link LiftDictionary} class.
 *
 * It allows to create a dictionary by setting its properties one by one.
 *
 * Usage:
 * <pre>
 *   // Create a dictionary :
 *   LiftDictionary dictionary = Builders.dictionary()
 *       .withLiftVersion(LiftVersion.V0_13)
 *       .withProducer("My language documentation project")
 *       .withMetaLanguages("en")
 *       .withObjectLanguages("qyz")
 *       .withDescription("en", "Description of the doculect X (code qyz)")
 *       .build();
 *  // And then start adding entry:
 *  Builder builder = dictionary.getComponentBuilder();
 *  builder.entry()
 *      .withId("entry1")
 *      .withForm("en", "entry1")
 *      .build();
 * </pre>
 */
public class LiftDictionaryBuilder {

    private final LiftDictionary dictionary;

    /**
     * Create a dictionary builder.
     */
    protected LiftDictionaryBuilder() {
        this.dictionary = new LiftDictionary();
    }

    public LiftDictionaryBuilder withLiftVersion(LiftVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.dictionary.liftVersion = version;
        return this;
    }

    public LiftDictionaryBuilder withProducer(String liftProducer) {
        if (liftProducer == null) {
            throw new IllegalArgumentException("liftProducer must not be null");
        }
        this.dictionary.liftProducer = liftProducer;
        return this;
    }

    public LiftDictionaryBuilder withDescription(
        String lang,
        String description
    ) {
        if (lang == null || description == null) {
            throw new IllegalArgumentException(
                "lang and description must not be null"
            );
        }
        this.dictionary.header
            .getDescription()
            .add(new Form(lang, description));
        return this;
    }

    public LiftDictionary build() {
        return this.dictionary;
    }

    public LiftDictionaryBuilder withMetaLanguages(String... langs) {
        for (String lang : langs)
            this.dictionary.getMetaLanguageManager().addLanguage(lang);
        return this;
    }

    public LiftDictionaryBuilder withObjectLanguages(String... langs) {
        for (String lang : langs)
            this.dictionary.getObjectLanguageManager().addLanguage(lang);
        return this;
    }

    public LiftDictionaryBuilder withPartOfSpeech(String... poss) {
        for (String pos : poss)
            this.dictionary.getHeader().getGrammaticalInfoManager().addFeature(pos);
        return this;
    }

    public LiftDictionaryBuilder withNoteType(String... noteTypes) {
        for (String noteType : noteTypes)
            this.dictionary.getHeader().getNoteTypeManager().addFeature(noteType);
        return this;
    }

    public LiftDictionaryBuilder withVariantType(String... variantTypes) {
        for (String variantType : variantTypes)
            this.dictionary.getHeader().getVariantTypeManager().addFeature(variantType);
        return this;
    }

}
