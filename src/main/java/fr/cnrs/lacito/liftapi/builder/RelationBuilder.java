package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.HasRelations;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.LiftRelation;

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
public class RelationBuilder extends AbstractLiftElementWithFieldBuilder<LiftRelation, HasRelations> {

    /**
     * Create a relation builder (requires type to be set later).
     * @param parent
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
            dictionary.getHeader().getRelationTypeManager().addFeature(type);
        }
        Feature e = dictionary.getHeader().getRelationTypeManager().getFeature(type);
        this.element.setType(e);
    }

    // /**
    //  * Set the relation ID.
    //  */
    // @Override
    // public RelationBuilder withId(String id) {
    //     super.withId(id);
    //     return this;
    // }

    // /**
    //  * Set the relation GUID.
    //  */
    // @Override
    // public RelationBuilder withGuid(String guid) {
    //     super.withGuid(guid);
    //     return this;
    // }

    /**
     * Set the reference ID (target of the relation).
     */
    public RelationBuilder withRef(AbstractIdentifiable target) {
        if (target == null) {
            throw new IllegalArgumentException("Null entry or sense cannot be referenced from a relation");
        } else {
            element.setRefObject(target);
        }
        return this;
    }

    // TODO create systematic type/typeId setter no all HasType builder; check during build that type is set
    public RelationBuilder withType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Relation type cannot be null or blank");
        }
        // if (!dictionary.getHeader().getRelationTypeManager().hasRangeElements(type)) {
        //     dictionary.getHeader().getRelationTypeManager().addFeature(type);
        // }
        Feature e = dictionary.getHeader().getRelationTypeManager().getFeature(type);

        super.withType(e);
        return this;
    }

    public RelationBuilder withRefId(String refId) {
        AbstractIdentifiable target = dictionary.getLiftDictionaryRegistry().getEntryOrSenseByLiftId(refId);
        return this.withRef(target);
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

    // Override WithField so that the correct type is returned
    
    @Override
    public RelationBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public RelationBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public RelationBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public RelationBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public RelationBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
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
