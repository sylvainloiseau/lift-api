package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.xml.LiftVersion;

/**
 * This class offers a fluent API for creating a {@link LiftDictionary}.
 *
 * It allows to create a dictionary by setting its properties one by one.
 *
 * Usage:
 * <pre>
 *
 *   // Create a dictionary :
 *   LiftDictionary dictionary = Builders.dictionary()
 *       .withLiftVersion(LiftVersion.V0_13)
 *       .withProducer("My language documentation project")
 *       .withMetaLanguages("en")
 *       .withObjectLanguages("qyz")
 *       .withDescription("en", "Description of the doculect X (code qyz)")
 *       .build();

 *  // Start adding entry:
 *  Builder builder = dictionary.getComponentBuilder();
 *  builder.entry()
 *      .withForm("en", "Run")
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

    // ------------------------------------------------------------------
    // languages
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Types for dictionary components
    // ------------------------------------------------------------------

    public LiftDictionaryBuilder withPartOfSpeech(String... poss) {
        for (String pos : poss)
            this.dictionary.getHeader().getGrammaticalInfoManager().addFeature(pos);
        return this;
    }

    public LiftDictionaryBuilder withNoteType(String... noteTypes) {
        addTypes(this.dictionary.getHeader().getNoteTypeManager(), noteTypes);
        return this;
    }

    public LiftDictionaryBuilder withPartOfSpeechType(String... posTypes) {
        addTypes(this.dictionary.getHeader().getGrammaticalInfoManager(), posTypes);
        return this;
    }

    public LiftDictionaryBuilder withVariantType(String... variantTypes) {
        addTypes(this.dictionary.getHeader().getVariantTypeManager(), variantTypes);
        return this;
    }

    public LiftDictionaryBuilder withTranslationType(String... translationTypes) {
        addTypes(this.dictionary.getHeader().getTranslationTypeManager(), translationTypes);
        return this;
    }

    public LiftDictionaryBuilder withRelationType(String... relationTypes) {
        addTypes(this.dictionary.getHeader().getRelationTypeManager(), relationTypes);
        return this;
    }

    public LiftDictionaryBuilder withReverseType(String... reverseTypes) {
        addTypes(this.dictionary.getHeader().getInverseTypeManager(), reverseTypes);
        return this;
    }

    public LiftDictionaryBuilder withAnnotationType(String... annotationTypes) {
        addTypes(this.dictionary.getHeader().getInverseTypeManager(), annotationTypes);
        return this;
    }

    public LiftDictionaryBuilder withEtymologyType(String... etymologyTypes) {
        addTypes(this.dictionary.getHeader().getEtymologyTypeManager(), etymologyTypes);
        return this;
    }

    private void addTypes(FeatureSet set, String[] types) {
        for (String type : types)
            set.addFeature(type);
    }

    // ------------------------------------------------------------------
    // Add FeatureSet
    // ------------------------------------------------------------------

    /**
     * Add a FeatureSet to the dictionary, that will be available later for
     * a {@link LiftFieldAndTraitDefinition}.
     */
    private void AddFeatureSet() {

    }
}
