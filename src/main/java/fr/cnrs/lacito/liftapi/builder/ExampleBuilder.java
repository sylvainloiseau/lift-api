package fr.cnrs.lacito.liftapi.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Forms given to {@link #addTranslation(String, Form)}, grouped by type.
     *
     * Several calls with the same type make up one translation, so a translation cannot
     * be built at the first call: once built it is registered, and its text then only
     * accepts the languages the dictionary already declares. They are built together in
     * {@link #build()}, where the translation's languages are declared on registration.
     */
    private final Map<Feature, List<Form>> pendingTranslations = new LinkedHashMap<>();

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
        requireNoBuiltTranslation(f);
        pendingTranslations.computeIfAbsent(f, k -> new ArrayList<>()).add(translation);
        return this;
    }

    /**
     * Add a translation via nested builder configuration.
     *
     * The translation is built at once, so a type cannot be given both here and to
     * {@link #addTranslation(String, Form)}.
     *
     * @throws IllegalStateException if a translation of this type was already added
     */
    public ExampleBuilder addTranslation(String type, Consumer<TranslationBuilder> config) {
        TranslationBuilder tb = new TranslationBuilder(dictionary, element, type);
        Feature f = tb.element.getType();
        if (pendingTranslations.containsKey(f)) {
            throw new IllegalStateException(
                "A translation of type " + f.getId() + " was already added to this example."
            );
        }
        requireNoBuiltTranslation(f);
        config.accept(tb);
        tb.build();
        return this;
    }

    /**
     * Refuse a translation type already built by the nested-builder overload: checking
     * here, rather than letting {@link #build()} fail, keeps the failure away from a
     * half-attached translation.
     */
    private void requireNoBuiltTranslation(Feature type) {
        if (element.getTranslations().containsKey(type)) {
            throw new IllegalStateException(
                "A translation of type " + type.getId() + " was already added to this example."
            );
        }
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
        for (Map.Entry<Feature, List<Form>> pending : pendingTranslations.entrySet()) {
            TranslationBuilder tb = new TranslationBuilder(dictionary, element, pending.getKey());
            pending.getValue().forEach(tb::addText);
            tb.build();
        }
        super.attach();
        return element;
    }
}
