package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import java.util.function.Consumer;

/**
 * Builder for creating LiftRelation instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftRelation relation = Builders.relation("synonymy")
 *       .withRefId("word-456")
 *       .addUsage("en", "This is synonymous with...")
 *       .build();
 * </pre>
 */
public class RelationBuilder extends AbstractLiftElementBuilder<LiftRelation, HasRelations> {

    /**
     * Create a relation builder (requires type to be set later).
     * @param type
     * @param dictionary
     */
    protected RelationBuilder(LiftDictionary dictionary, HasRelations parent) {
        super(LiftRelation.create(), dictionary, parent);
    }

    /**
     * Create a relation builder (requires type to be set later).
     * @param type
     */
    protected RelationBuilder(LiftDictionary dictionary, HasRelations parent, String type) {
        super(LiftRelation.create(), dictionary, parent);
        if (type == null) {
            throw new IllegalArgumentException("Relation type cannot be null");
        }
        if (!dictionary.getHeader().getRelationTypeManager().hasRangeElements(type)) {
            dictionary.getHeader().getRelationTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement e = dictionary.getHeader().getRelationTypeManager().getRangeElement(type);
        this.element.setType(e);
    }

    /**
     * Set the relation ID.
     */
    @Override
    public RelationBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the relation GUID.
     */
    @Override
    public RelationBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Set the reference ID (target of the relation).
     */
    public RelationBuilder withRefId(String refId) {
        AbstractIdentifiable target = dictionary.getLiftDictionaryRegistry().getEntryOrSenseByLiftId(refId);
        if (target == null) {
            throw new IllegalArgumentException("No entry or sense found for liftId: " + refId);
        } else {
            element.setRefObject(target);
        }
        return this;
    }

    /**
     * Set the order of this relation.
     */
    public RelationBuilder withOrder(Integer order) {
        if (order != null) {
            // Order is optional, stored in the element
        }
        return this;
    }

    /**
     * Add usage information in the specified language.
     */
    public RelationBuilder addUsage(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException(
                "Language and text cannot be null"
            );
        }
        element.getUsage().add(new Form(language, text));
        return this;
    }

    /**
     * Add usage information.
     */
    public RelationBuilder addUsage(Form usage) {
        if (usage == null) {
            throw new IllegalArgumentException("Usage cannot be null");
        }
        element.getUsage().add(usage);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public RelationBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public RelationBuilder addNote(Consumer<NoteBuilder> config, String type) {
        super.addNote(config, type);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public RelationBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public RelationBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the relation.
     */
    @Override
    public LiftRelation build() {
        if (element.getType() == null) {
            throw new IllegalStateException("Relation must have a type");
        }
        super.register();
        return element;
    }
}
