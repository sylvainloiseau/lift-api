package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.LiftField;

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
public class FieldBuilder extends AbstractLiftElementWithoutFieldBuilder<LiftField, HasField> {

    /**
     * Create a field builder with the given field name.
      * @param name
     */
    protected FieldBuilder(LiftDictionary dictionary, HasField field, String name) {
        super(LiftField.create(dictionary.getHeader().getOrCreateFieldDefinitions(name)), dictionary, field);
        if (name == null) {
            throw new IllegalArgumentException("Field name cannot be null");
        }
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

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public FieldBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public FieldBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }


    @Override
    public FieldBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the field.
     */
    @Override
    public LiftField build() {
        if (element.getSpecification() == null) {
            throw new IllegalArgumentException("Field name cannot be null");
        }
        super.register();
        return element;
    }
}
