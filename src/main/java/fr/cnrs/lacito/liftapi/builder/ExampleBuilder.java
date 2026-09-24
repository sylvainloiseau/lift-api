package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftSense;

/**
 * Builder for creating LiftExample instances with a fluent API.
 *
 * At least one form is the minimum requirement in order to build the entry.
 *
 * Usage:
 * <pre>
 *   LiftExample example = Builders.example()
 *       .withExample("en", "I found the word in the dictionary")
 *       .withSource("Webster's Dictionary")
 *       .addTranslation("French", "fr", "J'ai trouvé le mot dans le dictionnaire")
 *       .build();
 * </pre>
 */
public final class ExampleBuilder extends AbstractLiftElementWithFieldAndNoteBuilder<LiftExample, LiftSense> {

    protected ExampleBuilder(LiftDictionary dictionary, LiftSense parent) {
        super(LiftExample.create(), dictionary, parent);
    }

    /**
     * Create an example with a source.
     */
    protected ExampleBuilder(LiftDictionary dictionary, LiftSense parent, String source) {
        super(LiftExample.create(source), dictionary, parent);
    }

    /**
     * Add an example text in the specified language.
     */
    public ExampleBuilder withExample(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getExample().add(new Form(language, text));
        return this;
    }

    /**
     * Add an example text.
     */
    public ExampleBuilder withExample(Form example) {
        if (example == null) {
            throw new IllegalArgumentException("Example cannot be null");
        }
        element.getExample().add(example);
        return this;
    }

    /**
     * Set the source of this example.
     */
    public ExampleBuilder withSource(String source) {
        if (source != null) {
            element.setSource(source);
        }
        return this;
    }

    /**
     * Add a translation in the specified language.
     * The type is used to categorize translations (e.g. "free translation", "literal translation").
     */
    public ExampleBuilder addTranslation(String type, String language, String text) {
        return addTranslation(type, new Form(language, text));
    }

    /**
     * Add a translation.
     */
    public ExampleBuilder addTranslation(String type, Form translation) {
        if (type == null || translation == null) {
            throw new IllegalArgumentException("Type and translation cannot be null");
        }
        if (!dictionary.getHeader().getTranslationTypeManager().hasFeature(type)) {
            dictionary.getHeader().getTranslationTypeManager().addFeature(type);
        }
        Feature f = dictionary.getHeader().getTranslationTypeManager().getFeature(type);
        element.getOrCreateTranslation(f).add(translation);
        return this;
    }

    // Override addNote in order to return the good type

    @Override
    public ExampleBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    @Override
    public ExampleBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public ExampleBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public ExampleBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public ExampleBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public ExampleBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public ExampleBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the example.
     */
    @Override
    public LiftExample build() {
        if (element.getExample().isEmpty())
            throw new IllegalStateException("In order to build an example, it should contain at least one example");
        super.attach();
        return element;
    }
}
