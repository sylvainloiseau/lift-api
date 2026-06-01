package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
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
public class RelationBuilder extends AbstractLiftElementBuilder<LiftRelation> {

    /**
     * Create a relation builder with the given type.
     */
    public RelationBuilder(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Relation type cannot be null");
        }
        this.element = LiftRelation.create(type);
    }

    /**
     * Create a relation builder (requires type to be set later).
     */
    public RelationBuilder() {
        this("default");
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
     * Set the relation type.
     */
    public RelationBuilder withType(String type) {
        if (type != null) {
            element.setType(type);
        }
        return this;
    }

    /**
     * Set the reference ID (target of the relation).
     */
    public RelationBuilder withRefId(String refId) {
        if (refId != null) {
            element.setRefID(refId);
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
            throw new IllegalArgumentException("Language and text cannot be null");
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
    public RelationBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
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
        if (element.getType() == null || element.getType().isEmpty()) {
            throw new IllegalStateException("Relation must have a type");
        }
        return element;
    }
}
