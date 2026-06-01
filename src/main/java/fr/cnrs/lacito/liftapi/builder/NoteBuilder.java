package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import java.util.function.Consumer;

/**
 * Builder for creating LiftNote instances with a fluent API.
 * 
 * Usage:
 * <pre>
 *   LiftNote note = Builders.note()
 *       .withType("general")
 *       .addText("en", "This is a note")
 *       .build();
 * </pre>
 */
public class NoteBuilder extends AbstractLiftElementBuilder<LiftNote> {

    public NoteBuilder() {
        this.element = LiftNote.create();
    }

    /**
     * Create a note with a type.
     */
    public NoteBuilder(String type) {
        this.element = LiftNote.create(type);
    }

    /**
     * Set the note ID.
     */
    @Override
    public NoteBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the note GUID.
     */
    @Override
    public NoteBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Set the note type.
     */
    public NoteBuilder withType(String type) {
        if (type != null) {
            element.setType(type);
        }
        return this;
    }

    /**
     * Add text in the specified language.
     */
    public NoteBuilder addText(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getText().add(new Form(language, text));
        return this;
    }

    /**
     * Add text.
     */
    public NoteBuilder addText(Form text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        element.getText().add(text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public NoteBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public NoteBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public NoteBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public NoteBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the note.
     */
    @Override
    public LiftNote build() {
        return element;
    }
}
