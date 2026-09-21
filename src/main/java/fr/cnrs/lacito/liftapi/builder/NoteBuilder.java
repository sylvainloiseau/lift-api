package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftNote;

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
public class NoteBuilder extends AbstractLiftElementWithFieldBuilder<LiftNote, HasNote> {

    /**
     * Create a note with a type.
     */
    protected NoteBuilder(LiftDictionary dictionary, HasNote parent, String type) {
        super(LiftNote.create(), dictionary, parent);
        if (!dictionary.getHeader().getNoteTypeManager().hasFeature(type)) {
            dictionary.getHeader().getNoteTypeManager().addFeature(type);
        }
        Feature e = dictionary.getHeader().getNoteTypeManager().getFeature(type);
        element.setType(e);
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

    public NoteBuilder withType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Type cannot be null or empty.");
        // Create the type on demand, as this builder's own constructor already does.
        Feature f = dictionary
            .getHeader()
            .getNoteTypeManager()
            .getOrCreateFeature(type);
        super.withType(f);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public NoteBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public NoteBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public NoteBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public NoteBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public NoteBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
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
