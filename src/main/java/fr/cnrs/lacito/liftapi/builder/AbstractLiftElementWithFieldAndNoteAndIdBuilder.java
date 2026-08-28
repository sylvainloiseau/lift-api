package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractLiftRoot;
import fr.cnrs.lacito.liftapi.model.Identifiable;
import fr.cnrs.lacito.liftapi.model.LiftObject;

/**
 * Super class for the builders of the components that also have IDs.
 *
 * @param <T> the type of the component built
 * @param <U> the type of the parent of the component built in the dictionary structure
 */
public abstract sealed class AbstractLiftElementWithFieldAndNoteAndIdBuilder<T extends AbstractLiftRoot, U extends LiftObject>
    extends AbstractLiftElementWithFieldAndNoteBuilder<T, U>
    permits EntryBuilder, SenseBuilder
    {
    
    protected AbstractLiftElementWithFieldAndNoteAndIdBuilder(T element, LiftDictionary dictionary, U parent) {
        super(element, dictionary, parent);
    }

    /**
     * Set the element ID (for identifiable elements: Entry and Sense).
     */
    public AbstractLiftElementWithFieldAndNoteAndIdBuilder<T, U> withId(String id) {
        if (element instanceof Identifiable i) {
            i.setId(id);
        } else {
            throw new IllegalArgumentException(
                "Cannot set ID on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }

    /**
     * Set the element GUID (for identifiable elements: Entry and Sense).
     */
    public AbstractLiftElementWithFieldAndNoteAndIdBuilder<T, U> withGuid(String guid) {
        if (element instanceof Identifiable i) {
            i.setGuid(guid);
        } else {
            throw new IllegalArgumentException(
                "Cannot set Guid on an element of type: " + element.getClass().getName()
            );
        }
        return this;
    }


}
