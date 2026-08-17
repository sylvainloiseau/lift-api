package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithField;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasNote;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.Identifiable;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftObject;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

import java.util.function.Consumer;


/**
 * Abstract base class for all LIFT element builders.
 * Provides common functionality for building LIFT model elements with a fluent API.
 *
 * @param <T> the type of LIFT element being built
 * @param <U> the type of the parent element
 */
public abstract class AbstractLiftElementBuilder<T extends AbstractLiftRoot, U extends LiftObject> {

    protected final T element;
    protected final LiftDictionaryRegistry registry;
    protected final LiftDictionary dictionary;
    protected final U parent;

    protected AbstractLiftElementBuilder(T element, LiftDictionary dictionary, U parent) {
        this.element = element;
        this.registry = dictionary.getLiftDictionaryRegistry();
        this.dictionary = dictionary;
        this.parent = parent;
    }

    /**
     * Set the element ID (for identifiable elements).
     * @throws IllegalArgumentException if the element built is not an instance of Identifiable
     */
    public AbstractLiftElementBuilder<T, U> withId(String id) {
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
    public AbstractLiftElementBuilder<T, U> withGuid(String guid) {
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
    public AbstractLiftElementBuilder<T, U> dateCreated(String date) {
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
    public AbstractLiftElementBuilder<T, U> dateModified(String date) {
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
    public AbstractLiftElementBuilder<T, U> addNote(
        String type,
        String language,
        String text
    ) {
        if (element instanceof HasNote) {
            LiftNote note = new NoteBuilder(dictionary, (HasNote)element, type).build();
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
    public AbstractLiftElementBuilder<T, U> addNote(Consumer<NoteBuilder> config, String type) {
        if (element instanceof HasNote) {
            NoteBuilder nb = new NoteBuilder(dictionary, (HasNote) element, type);
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
    public AbstractLiftElementBuilder<T, U> addTrait(String name, String value) {
        if (element instanceof HasTrait) {
            LiftFieldAndTraitDefinition definition = dictionary.getHeader().getFieldsAndTraitsDefinitions(name);
            LiftTrait trait = new LiftTrait(definition, value);
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
    public AbstractLiftElementBuilder<T, U> addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        if (element instanceof HasTrait) {
            TraitBuilder tb = new TraitBuilder(dictionary, (HasTrait) element, name, value);
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
    public AbstractLiftElementBuilder<T, U> addField(
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
    public AbstractLiftElementBuilder<T, U> addField(
        String name,
        Consumer<FieldBuilder> config
    ) {
        if (element instanceof AbstractExtensibleWithField) {
            FieldBuilder fb = new FieldBuilder(dictionary, (AbstractExtensibleWithField) element, name);
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
    public AbstractLiftElementBuilder<T, U> addAnnotation(
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

    /**
     * Register the element in the registry, and add the object to its parent
     * (the parent take care of creating the reference from the child towards
     * itself).
     */
    protected void register() {
        switch(element){
            case LiftNote note -> {
                ((AbstractNotable)parent).addNote(note);
            }
            case LiftEntry _ -> { }
            case LiftSense sense -> {
                ((HasSense)parent).addSense(sense);
            }
            case LiftVariant variant -> {
                ((LiftEntry)parent).addVariant(variant);
            }
            case LiftPronunciation pronunciation -> {
                ((HasPronunciation)parent).addPronunciation(pronunciation);
            }
            case LiftExample example -> {
                ((LiftSense)parent).addExample(example);
            }
            case LiftField field -> {
                ((HasField)parent).addField(field);
            }
            case LiftAnnotation annotation -> {
                ((HasAnnotation)parent).addAnnotation(annotation);
            }
            default -> {}
        }
        registry.register(this.element);
    }
}
