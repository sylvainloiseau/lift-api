package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import java.util.function.Consumer;

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
public class PronunciationBuilder extends AbstractLiftElementBuilder<LiftPronunciation> {

    public PronunciationBuilder() {
        this.element = LiftPronunciation.create();
    }

    /**
     * Set the pronunciation ID.
     */
    @Override
    public PronunciationBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the pronunciation GUID.
     */
    @Override
    public PronunciationBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
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

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public PronunciationBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public PronunciationBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public PronunciationBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public PronunciationBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the pronunciation.
     */
    @Override
    public LiftPronunciation build() {
        return element;
    }
}
