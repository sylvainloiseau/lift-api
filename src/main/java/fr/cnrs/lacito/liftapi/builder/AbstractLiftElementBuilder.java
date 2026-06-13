package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.Identifiable;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Abstract base class for all LIFT element builders.
 * Provides common functionality for building LIFT model elements with a fluent API.
 *
 * @param <T> the type of LIFT element being built
 */
public abstract class AbstractLiftElementBuilder<T extends AbstractLiftRoot> {

    protected T element;
    protected LiftDictionaryRegistry registry;

    /**
     * Set the element ID (for identifiable elements).
     * @throws IllegalArgumentException if the element built is not an instance of Identifiable
     */
    public AbstractLiftElementBuilder<T> withId(String id) {
        if (element instanceof Identifiable i) {
            i.setId(id);
        } else {
            throw new IllegalArgumentException("Cannot add ID on this element");
        }
        return this;
    }

    /**
     * Set the element GUID (for identifiable elements).
     * @throws IllegalArgumentException if the element built is not an instance of Identifiable
     */
    public AbstractLiftElementBuilder<T> withGuid(String guid) {
        if (element instanceof Identifiable i) {
            i.setGuid(guid);
        } else {
            throw new IllegalArgumentException(
                "Cannot add Guid on this element"
            );
        }
        return this;
    }

    /**
     * Set the date created (for extensible elements).
     *
     * @throws IllegalArgumentException if the element built is not an instance of AbstractExtensibleWithoutField and cannot receive creation date
     */
    public AbstractLiftElementBuilder<T> dateCreated(String date) {
        if (element instanceof AbstractExtensibleWithoutField) {
            ((AbstractExtensibleWithoutField) element).setDateCreated(date);
        } else {
            throw new IllegalArgumentException(
                "Cannot add creation date on this element"
            );
        }
        return this;
    }

    /**
     * Set the date modified (for extensible elements).
     *
     * @throws IllegalArgumentException if the element built is not an instance of AbstractExtensibleWithoutField and cannot receive modification date
     */
    public AbstractLiftElementBuilder<T> dateModified(String date) {
        if (element instanceof AbstractExtensibleWithoutField) {
            ((AbstractExtensibleWithoutField) element).setDateModified(date);
        } else {
            throw new IllegalArgumentException(
                "Cannot add creation date on this element"
            );
        }
        return this;
    }

    /**
     * Add a note with type, language, and text.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasNote.
     */
    public AbstractLiftElementBuilder<T> addNote(
        String type,
        String language,
        String text
    ) {
        if (element instanceof HasNote) {
            LiftNote note = LiftNote.create();
            if (type != null && !type.isEmpty()) {
                note.setType(type);
            }
            note.addText(new Form(language, text));
            ((HasNote) element).addNote(note);
        } else {
            throw new IllegalArgumentException(
                "Cannot add note on this element"
            );
        }
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasNote.
     */
    public AbstractLiftElementBuilder<T> addNote(Consumer<NoteBuilder> config) {
        if (element instanceof HasNote) {
            NoteBuilder nb = new NoteBuilder(this.registry);
            config.accept(nb);
            ((HasNote) element).addNote(nb.build());
        } else {
            throw new IllegalArgumentException(
                "Cannot add note on this element"
            );
        }
        return this;
    }

    /**
     * Add a trait with name and value.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasTrait.
     */
    public AbstractLiftElementBuilder<T> addTrait(String name, String value) {
        if (element instanceof HasTrait) {
            LiftTrait trait = LiftTrait.create(name, value);
            ((HasTrait) element).addTrait(trait);
        } else {
            throw new IllegalArgumentException(
                "Cannot add trait on this element"
            );
        }
        return this;
    }

    /**
     * Add a trait with name, value, and annotations.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasTrait.
     */
    public AbstractLiftElementBuilder<T> addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        if (element instanceof HasTrait) {
            TraitBuilder tb = new TraitBuilder(this.registry, name, value);
            config.accept(tb);
            ((HasTrait) element).addTrait(tb.build());
        } else {
            throw new IllegalArgumentException(
                "Cannot add trait on this element"
            );
        }
        return this;
    }

    /**
     * Add a field with name, language, and text.
     *
     * @throws IllegalArgumentException if the element built cannot received field.
     */
    public AbstractLiftElementBuilder<T> addField(
        String name,
        String language,
        String text
    ) {
        if (element instanceof AbstractExtensibleWithField) {
            LiftField field = LiftField.create(name);
            field.addText(new Form(language, text));
            ((AbstractExtensibleWithField) element).addField(field);
        } else {
            throw new IllegalArgumentException(
                "Cannot add field on this element"
            );
        }
        return this;
    }

    /**
     * Add a field via nested builder configuration.
     *
     * @throws IllegalArgumentException if the element built cannot received field.
     */
    public AbstractLiftElementBuilder<T> addField(
        String name,
        Consumer<FieldBuilder> config
    ) {
        if (element instanceof AbstractExtensibleWithField) {
            FieldBuilder fb = new FieldBuilder(this.registry, name);
            config.accept(fb);
            ((AbstractExtensibleWithField) element).addField(fb.build());
        } else {
            throw new IllegalArgumentException(
                "Cannot add field on this element"
            );
        }
        return this;
    }

    /**
     * Add an annotation to the element.
     * @throws IllegalArgumentException if the element built is not an instance of HasAnnotation
     */
    public AbstractLiftElementBuilder<T> addAnnotation(
        String name,
        String value
    ) {
        if (element instanceof HasAnnotation) {
            LiftAnnotation annotation = LiftAnnotation.create(name, value);
            ((HasAnnotation) element).addAnnotation(annotation);
        } else {
            throw new IllegalArgumentException(
                "Cannot add annotation on this element"
            );
        }
        return this;
    }

    /**
     * Build the element. Subclasses should override to add validation.
     */
    public abstract T build();

    protected void register() {
        UUID uuid = registry.getNewUUID();
        this.element.setUUID(uuid);
        registry.register(this.element);
    }
}
