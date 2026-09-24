package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;

/**
 * Builder for creating LiftPronunciation instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftPronunciation pronunciation = Builders.pronunciation()
 *       .withPronunciation("en", "ˈdɪkʃəneri")
 *       .build();
 * </pre>
 */
public class PronunciationBuilder extends AbstractLiftElementWithFieldBuilder<LiftPronunciation, HasPronunciation> {

    protected PronunciationBuilder(LiftDictionary dictionary, HasPronunciation parent) {
        super(LiftPronunciation.create(), dictionary, parent);
    }

    /**
     * Add a pronunciation form in the specified language.
     */
    public PronunciationBuilder withPronunciation(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getPronunciation().add(new Form(language, text));
        return this;
    }

    /**
     * Add a pronunciation form.
     */
    public PronunciationBuilder withPronunciation(Form pronunciation) {
        if (pronunciation == null) {
            throw new IllegalArgumentException("Pronunciation cannot be null");
        }
        element.getPronunciation().add(pronunciation);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public PronunciationBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public PronunciationBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public PronunciationBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public PronunciationBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public PronunciationBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the pronunciation.
     */
    @Override
    public LiftPronunciation build() {
        super.attach();
        return element;
    }
}
