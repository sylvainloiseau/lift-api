package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import java.util.function.Consumer;

/**
 * Builder for creating LiftExample instances with a fluent API.
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
public class ExampleBuilder extends AbstractLiftElementBuilder<LiftExample> {

    public ExampleBuilder() {
        this.element = LiftExample.create();
    }

    /**
     * Create an example with a source.
     */
    public ExampleBuilder(String source) {
        this.element = LiftExample.create(source);
    }

    /**
     * Set the example ID.
     */
    @Override
    public ExampleBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the example GUID.
     */
    @Override
    public ExampleBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
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
        if (type == null || language == null || text == null) {
            throw new IllegalArgumentException("Type, language and text cannot be null");
        }
        element.getOrCreateTranslation(type).add(new Form(language, text));
        return this;
    }

    /**
     * Add a translation.
     */
    public ExampleBuilder addTranslation(String type, Form translation) {
        if (type == null || translation == null) {
            throw new IllegalArgumentException("Type and translation cannot be null");
        }
        element.getOrCreateTranslation(type).add(translation);
        return this;
    }


    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public ExampleBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public ExampleBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public ExampleBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public ExampleBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the example.
     */
    @Override
    public LiftExample build() {
        return element;
    }
}
