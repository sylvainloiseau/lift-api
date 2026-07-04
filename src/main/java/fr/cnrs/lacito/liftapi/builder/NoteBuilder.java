package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;
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

    /**
     * Create a note with a type.
     */
    protected NoteBuilder(LiftDictionary dictionary, String type) {
        this.registry = dictionary.getLiftDictionaryRegistry();
        if (!dictionary.getHeader().containsNoteType(type)) {
            dictionary.getHeader().addNoteType(type);
        }
        LiftHeaderRangeElement e = dictionary.getHeader().getNoteType(type);
        this.element = LiftNote.create(e);
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
    public NoteBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
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
        super.register();
        return element;
    }
}
