package fr.cnrs.lacito.liftapi;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftHeader;

/**
 * This class is a builder for the {@LiftDictionary} class.
 *
 * It allows to create a dictionary by setting its properties one by one.
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
 */
public class LiftDictionaryBuilder {

    private final LiftDictionary dictionary;

    /**
     * Create a dictionary builder.
     */
    protected LiftDictionaryBuilder() {
        this.dictionary = new LiftDictionary();
    }

    public LiftDictionaryBuilder withLiftVersion(String version) {
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
}
