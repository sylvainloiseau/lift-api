package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
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
public class VariantBuilder extends AbstractLiftElementBuilder<LiftVariant> {

    public VariantBuilder() {
        this.element = LiftVariant.create();
    }

    /**
     * Set the variant ID.
     */
    @Override
    public VariantBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the variant GUID.
     */
    @Override
    public VariantBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Add a form in the specified language.
     */
    public VariantBuilder withForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.addForm(new Form(language, text));
        return this;
    }

    /**
     * Add a form.
     */
    public VariantBuilder withForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.addForm(form);
        return this;
    }

    /**
     * Set the reference ID for this variant.
     */
    public VariantBuilder withRefId(String refId) {
        if (refId != null) {
            // Use reflection to call protected setRefId method
            try {
                java.lang.reflect.Method method = LiftVariant.class.getDeclaredMethod("setRefId", String.class);
                method.setAccessible(true);
                method.invoke(element, refId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set refId", e);
            }
        }
        return this;
    }

    /**
     * Add a pronunciation via nested builder configuration.
     */
    public VariantBuilder addPronunciation(Consumer<PronunciationBuilder> config) {
        PronunciationBuilder pb = new PronunciationBuilder();
        config.accept(pb);
        element.addPronunciation(pb.build());
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
    public VariantBuilder addRelation(Consumer<RelationBuilder> config) {
        RelationBuilder rb = new RelationBuilder();
        config.accept(rb);
        element.addRelation(rb.build());
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

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public VariantBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public VariantBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public VariantBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public VariantBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the variant.
     */
    @Override
    public LiftVariant build() {
        return element;
    }
}
