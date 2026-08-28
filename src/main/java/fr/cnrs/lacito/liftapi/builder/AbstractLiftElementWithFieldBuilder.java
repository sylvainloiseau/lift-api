package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftObject;

/**
 * Super class for the builders of the components that also have {@link LiftField}.
 *
 * @param <T> the type of the component built
 * @param <U> the type of the parent of the component built in the dictionary structure
 */
public abstract class AbstractLiftElementWithFieldBuilder<T extends AbstractLiftRoot, U extends LiftObject> extends AbstractLiftElementWithoutFieldBuilder<T, U> {
    
    protected AbstractLiftElementWithFieldBuilder(T element, LiftDictionary dictionary, U parent) {
        super(element, dictionary, parent);
    }

    /**
     * Add a field with name, language, and text.
     *
     * @throws IllegalArgumentException if the element built cannot received field.
     */
    public AbstractLiftElementWithFieldBuilder<T, U> addField(
        String name,
        String language,
        String text
    ) {
        if (element instanceof AbstractExtensibleWithField parent) {
            new FieldBuilder(dictionary, parent, name).addText(language, text).build();
            // LiftField field = LiftField.create(name);
            // field.addText(new Form(language, text));
            // ((AbstractExtensibleWithField) element).addField(field);
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftField on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Add a field via nested builder configuration.
     *
     * @throws IllegalArgumentException if the element built cannot received field.
     */
    public AbstractLiftElementWithFieldBuilder<T, U> addField(
        String name,
        Consumer<FieldBuilder> config
    ) {
        if (element instanceof AbstractExtensibleWithField parent) {
            FieldBuilder fb = new FieldBuilder(dictionary, parent, name);
            config.accept(fb);
            fb.build();
            // No : already added by NoteBuilder!
            //((AbstractExtensibleWithField) element).addField(fb.build());
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftField on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

}
