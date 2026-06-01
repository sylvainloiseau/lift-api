package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import java.util.function.Consumer;

/**
 * Abstract base class for all LIFT element builders.
 * Provides common functionality for building LIFT model elements with a fluent API.
 *
 * @param <T> the type of LIFT element being built
 */
public abstract class AbstractLiftElementBuilder<T extends AbstractLiftRoot> {
    protected T element;

    /**
     * Set the element ID (for identifiable elements).
     */
    public AbstractLiftElementBuilder<T> withId(String id) {
        if (element instanceof AbstractIdentifiable) {
            ((AbstractIdentifiable) element).setId(id);
        }
        return this;
    }

    /**
     * Set the element GUID (for identifiable elements).
     */
    public AbstractLiftElementBuilder<T> withGuid(String guid) {
        if (element instanceof AbstractIdentifiable) {
            ((AbstractIdentifiable) element).setGuid(guid);
        }
        return this;
    }

    /**
     * Set the date created (for extensible elements).
     */
    public AbstractLiftElementBuilder<T> dateCreated(String date) {
        if (element instanceof AbstractExtensibleWithoutField) {
            ((AbstractExtensibleWithoutField) element).setDateCreated(date);
        }
        return this;
    }

    /**
     * Set the date modified (for extensible elements).
     */
    public AbstractLiftElementBuilder<T> dateModified(String date) {
        if (element instanceof AbstractExtensibleWithoutField) {
            ((AbstractExtensibleWithoutField) element).setDateModified(date);
        }
        return this;
    }

    /**
     * Add a note with type, language, and text.
     */
    public AbstractLiftElementBuilder<T> addNote(String type, String language, String text) {
        if (element instanceof HasNote) {
            LiftNote note = LiftNote.create();
            if (type != null && !type.isEmpty()) {
                note.setType(type);
            }
            note.addText(new Form(language, text));
            ((HasNote) element).addNote(note);
        }
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    public AbstractLiftElementBuilder<T> addNote(Consumer<NoteBuilder> config) {
        if (element instanceof HasNote) {
            NoteBuilder nb = new NoteBuilder();
            config.accept(nb);
            ((HasNote) element).addNote(nb.build());
        }
        return this;
    }

    /**
     * Add a trait with name and value.
     */
    public AbstractLiftElementBuilder<T> addTrait(String name, String value) {
        if (element instanceof HasTrait) {
            LiftTrait trait = LiftTrait.create(name, value);
            ((HasTrait) element).addTrait(trait);
        }
        return this;
    }

    /**
     * Add a trait with name, value, and annotations.
     */
    public AbstractLiftElementBuilder<T> addTrait(String name, String value, Consumer<TraitBuilder> config) {
        if (element instanceof HasTrait) {
            TraitBuilder tb = new TraitBuilder(name, value);
            config.accept(tb);
            ((HasTrait) element).addTrait(tb.build());
        }
        return this;
    }

    /**
     * Add a field with name, language, and text.
     */
    public AbstractLiftElementBuilder<T> addField(String name, String language, String text) {
        if (element instanceof AbstractExtensibleWithField) {
            LiftField field = LiftField.create(name);
            field.addText(new Form(language, text));
            ((AbstractExtensibleWithField) element).addField(field);
        }
        return this;
    }

    /**
     * Add a field via nested builder configuration.
     */
    public AbstractLiftElementBuilder<T> addField(String name, Consumer<FieldBuilder> config) {
        if (element instanceof AbstractExtensibleWithField) {
            FieldBuilder fb = new FieldBuilder(name);
            config.accept(fb);
            ((AbstractExtensibleWithField) element).addField(fb.build());
        }
        return this;
    }

    /**
     * Add an annotation to the element.
     */
    public AbstractLiftElementBuilder<T> addAnnotation(String name, String value) {
        if (element instanceof HasAnnotation) {
            LiftAnnotation annotation = LiftAnnotation.create(name, value);
            ((HasAnnotation) element).addAnnotation(annotation);
        }
        return this;
    }

    /**
     * Build the element. Subclasses should override to add validation.
     */
    public abstract T build();
}
