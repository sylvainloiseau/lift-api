package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftObject;

/**
 * Super class for the builders of the components that also have {@link LiftNote}.
 *
 * @param <T> the type of the component built
 * @param <U> the type of the parent of the component built in the dictionary structure
 */
public abstract sealed class AbstractLiftElementWithFieldAndNoteBuilder<T extends AbstractLiftRoot, U extends LiftObject>
    extends AbstractLiftElementWithFieldBuilder<T, U>
    permits ExampleBuilder, AbstractLiftElementWithFieldAndNoteAndIdBuilder
    {
    
    protected AbstractLiftElementWithFieldAndNoteBuilder(T element, LiftDictionary dictionary, U parent) {
        super(element, dictionary, parent);
    }

    /**
     * Add a note with type, language, and text.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasNote.
     */
    public AbstractLiftElementWithFieldAndNoteBuilder<T, U> addNote(
        String type,
        String language,
        String text
    ) {
        if (element instanceof HasNote parent) {
            new NoteBuilder(dictionary, parent, type).addText(language, text).build();
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftNote on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasNote.
     */
    public AbstractLiftElementWithFieldAndNoteBuilder<T, U> addNote(Consumer<NoteBuilder> config, String type) {
        if (element instanceof HasNote parent) {
            NoteBuilder nb = new NoteBuilder(dictionary, parent, type);
            config.accept(nb);
            nb.build();
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftNote on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

}
