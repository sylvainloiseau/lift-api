package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftObject;
import fr.cnrs.lacito.liftapi.model.LiftTrait;

/**
 * Super class for the builders of the components that have {@link LiftTrait}, {@link LiftAnnotation}, as well as creation date and modification date.
 *
 * @param <T> the type of the component built
 * @param <U> the type of the parent of the component built in the dictionary structure
 */
public abstract class AbstractLiftElementWithoutFieldBuilder<T extends AbstractLiftRoot, U extends LiftObject> extends AbstractLiftElementBuilder<T, U> {
    
    protected AbstractLiftElementWithoutFieldBuilder(T element, LiftDictionary dictionary, U parent) {
        super(element, dictionary, parent);
    }

    /**
     * Set the date created (for extensible elements).
     *
     * @throws IllegalArgumentException if the element built is not an instance of AbstractExtensibleWithoutField and cannot receive creation date
     */
    public AbstractLiftElementWithoutFieldBuilder<T, U> dateCreated(String date) {
        if (element instanceof AbstractExtensibleWithoutField i) {
            i.setDateCreated(date);
        } else {
            throw new IllegalArgumentException(
                "Cannot set creation date on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Set the date modified (for extensible elements).
     *
     * @throws IllegalArgumentException if the element built is not an instance of AbstractExtensibleWithoutField and cannot receive modification date
     */
    public AbstractLiftElementWithoutFieldBuilder<T, U> dateModified(String date) {
        if (element instanceof AbstractExtensibleWithoutField i) {
            i.setDateModified(date);
        } else {
            throw new IllegalArgumentException(
                "Cannot set modification date on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Add a trait with name and value.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasTrait.
     */
    public AbstractLiftElementWithoutFieldBuilder<T, U> addTrait(String name, String value) {
        if (element instanceof HasTrait parent) {
            new TraitBuilder(dictionary, parent, name, value).build();
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftTrait on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Add a trait with name, value, and annotations.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasTrait.
     */
    public AbstractLiftElementWithoutFieldBuilder<T, U> addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        if (element instanceof HasTrait parent) {
            TraitBuilder tb = new TraitBuilder(dictionary, parent, name, value);
            config.accept(tb);
            tb.build();
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftTrait on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }


    /**
     * Add an annotation to the element.
     * @throws IllegalArgumentException if the element built is not an instance of HasAnnotation
     */
    public AbstractLiftElementWithoutFieldBuilder<T, U> addAnnotation(
        String name,
        String value
    ) {
        if (element instanceof HasAnnotation parent) {
            new AnnotationBuilder(dictionary, parent, name).withValue(value).build();
        } else {
            throw new IllegalArgumentException(
                "Cannot add LiftAnnotation on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

}
