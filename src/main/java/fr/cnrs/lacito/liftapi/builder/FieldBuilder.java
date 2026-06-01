package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftField;
import java.util.function.Consumer;

/**
 * Builder for creating LiftField instances with a fluent API.
 * 
 * Usage:
 * <pre>
 *   LiftField field = Builders.field("customData")
 *       .addText("en", "some custom value")
 *       .build();
 * </pre>
 */
public class FieldBuilder extends AbstractLiftElementBuilder<LiftField> {

    /**
     * Create a field builder with the given field name.
     */
    public FieldBuilder(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Field name cannot be null");
        }
        this.element = LiftField.create(name);
    }

    /**
     * Set the field ID.
     */
    @Override
    public FieldBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the field GUID.
     */
    @Override
    public FieldBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Add text in the specified language.
     */
    public FieldBuilder addText(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getText().add(new Form(language, text));
        return this;
    }

    /**
     * Add text.
     */
    public FieldBuilder addText(Form text) {
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
    public FieldBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public FieldBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public FieldBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public FieldBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the field.
     */
    @Override
    public LiftField build() {
        return element;
    }
}
