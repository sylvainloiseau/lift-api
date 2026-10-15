package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftVariant;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import java.util.function.Consumer;

/**
 * Builder for creating LiftVariant instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftVariant variant = Builders.variant()
 *       .withForm("en", "alternate spelling")
 *       .addPronunciation(p -> p
 *           .withPronunciation("en", "ɔːltərnət")
 *       )
 *       .build();
 * </pre>
 */
public class VariantBuilder extends AbstractLiftElementWithFieldBuilder<LiftVariant, LiftEntry> {

    protected VariantBuilder(LiftDictionary dictionary, LiftEntry parent) {
        super(new LiftVariant(), dictionary, parent);
    }

    /**
     * Add a form in the specified language.
     */
    public VariantBuilder withForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getForms().add(new Form(language, text));
        return this;
    }

    /**
     * Add a form.
     */
    public VariantBuilder withForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.getForms().add(form);
        return this;
    }


    /**
     * Set the LIFT id of the entry or sense this variant refers to.
     *
     * The target need not exist yet: the id is recorded, and the variant points to the
     * target object at once if the dictionary already holds it, or later through
     * {@link LiftDictionary#resolveReferences()} (see
     * {@link RelationBuilder#withRefId(String)}).
     *
     * @param refId the LIFT id of the target
     * @throws IllegalArgumentException if {@code refId} is null or blank
     */
    public VariantBuilder withRefId(String refId) {
        if (refId == null || refId.isBlank()) {
            throw new IllegalArgumentException("The id referred to cannot be null or blank");
        }
        element.setRefId(refId);
        AbstractIdentifiable target = dictionary.getLiftDictionaryRegistry().getEntryOrSenseByLiftId(refId);
        if (target != null) {
            element.setRefObject(target);
        }
        return this;
    }

    /**
     * Add a pronunciation via nested builder configuration.
     */
    public VariantBuilder addPronunciation(Consumer<PronunciationBuilder> config) {
        PronunciationBuilder pb = new PronunciationBuilder(dictionary, element);
        config.accept(pb);
        // build() registers the pronunciation and attaches it to this variant.
        pb.build();
        return this;
    }

    /**
     * Add a pronunciation directly.
     */
    public VariantBuilder addPronunciation(LiftPronunciation pronunciation) {
        if (pronunciation == null) {
            throw new IllegalArgumentException("Pronunciation cannot be null");
        }
        element.addPronunciation(pronunciation);
        return this;
    }

    /**
     * Add a relation via nested builder configuration.
     */
    public VariantBuilder addRelation(Consumer<RelationBuilder> config, String type) {
        RelationBuilder rb = new RelationBuilder(dictionary, element, type);
        config.accept(rb);
        // build() registers the relation and attaches it to this variant.
        rb.build();
        return this;
    }

    /**
     * Add a relation directly.
     */
    public VariantBuilder addRelation(LiftRelation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("Relation cannot be null");
        }
        element.addRelation(relation);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public VariantBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public VariantBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public VariantBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public VariantBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public VariantBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the variant.
     */
    @Override
    public LiftVariant build() {
        super.attach();
        return element;
    }
}
