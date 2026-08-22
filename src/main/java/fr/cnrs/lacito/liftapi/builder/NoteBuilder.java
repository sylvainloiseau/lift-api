package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasNote;
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
public class NoteBuilder extends AbstractLiftElementBuilder<LiftNote, HasNote> {

    /**
     * Create a note with a type.
     */
    protected NoteBuilder(LiftDictionary dictionary, HasNote parent, String type) {
        super(LiftNote.create(), dictionary, parent);
        if (!dictionary.getHeader().getNoteTypeManager().hasRangeElements(type)) {
            dictionary.getHeader().getNoteTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement e = dictionary.getHeader().getNoteTypeManager().getRangeElement(type);
        element.setType(e);
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
        if (element.getType() == null) {
            throw new IllegalStateException("Note type cannot be null");
        }
        super.register();
        return element;
    }
}
