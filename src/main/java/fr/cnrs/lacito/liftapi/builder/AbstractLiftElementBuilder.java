package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.AbstractNotable;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasField;
import fr.cnrs.lacito.liftapi.model.HasPronunciation;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.HasSense;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.HasType;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftNote;
import fr.cnrs.lacito.liftapi.model.LiftObject;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

/**
 * Abstract base class for all LIFT element builders.
 * 
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
    private boolean registered = false;

    protected AbstractLiftElementBuilder(T element, LiftDictionary dictionary, U parent) {
        this.element = element;
        this.registry = dictionary.getLiftDictionaryRegistry();
        this.dictionary = dictionary;
        this.parent = parent;
    }

    public AbstractLiftElementBuilder<T, U> addMultitext(String lang, String text) {
        if (lang == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getMainMultiText().add(new Form(lang, text));
        return this;
    }

    public AbstractLiftElementBuilder<T, U> addMultitext(Form text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        element.getMainMultiText().add(text);
        return this;
    }


    /**
     * Set the element type (for  components implementing {@link HasType}).
     * @throws IllegalArgumentException if the element built is not an instance of {@code HasType}
     */
    public AbstractLiftElementBuilder<T, U> withType(Feature type) {
        if (element instanceof HasType ht) {
            ht.setType(type);
        } else {
            throw new IllegalArgumentException(
                "Cannot set a type on an component of type: " + element.getClass().getName()
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
        if(registered) {
            throw new IllegalStateException("This builder has already been used.");
        }
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
            case LiftTrait trait -> {
                ((HasTrait)parent).addTrait(trait);
            }
            case LiftRelation relation -> {
                ((HasRelations)parent).addRelation(relation);
            }
            case LiftEtymology etymology -> {
                ((LiftEntry)parent).addEtymology(etymology);
            }
            default -> {throw new IllegalArgumentException("Unsupported element type: " + element);}
        }
        registry.register(this.element);
        this.registered = true;
    }
}
