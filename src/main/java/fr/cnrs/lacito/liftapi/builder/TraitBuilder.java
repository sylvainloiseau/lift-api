package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.LiftTrait;
import java.util.function.Consumer;

/**
 * Builder for creating LiftTrait instances with a fluent API.
 * 
 * Usage:
 * <pre>
 *   LiftTrait trait = Builders.trait("category", "noun")
 *       .build();
 * </pre>
 */
public class TraitBuilder extends AbstractLiftElementBuilder<LiftTrait> {

    /**
     * Create a trait builder with the given name and value.
     */
    public TraitBuilder(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("Trait name and value cannot be null");
        }
        this.element = LiftTrait.create(name, value);
    }

    /**
     * Set the trait ID.
     */
    @Override
    public TraitBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the trait GUID.
     */
    @Override
    public TraitBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Update the trait value.
     */
    public TraitBuilder withValue(String value) {
        if (value != null) {
            element.setValue(value);
        }
        return this;
    }

    /**
     * Add an annotation via nested builder configuration.
     */
    public TraitBuilder addAnnotation(Consumer<AnnotationBuilder> config) {
        AnnotationBuilder ab = new AnnotationBuilder();
        config.accept(ab);
        element.getAnnotations().add(ab.build());
        return this;
    }

    /**
     * Add an annotation with name and optional value.
     */
    public TraitBuilder addAnnotation(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Annotation name cannot be null");
        }
        if (value == null) {
            element.getAnnotations().add(AnnotationBuilder.createAnnotation(name));
        } else {
            element.getAnnotations().add(AnnotationBuilder.createAnnotation(name, value));
        }
        return this;
    }

    /**
     * Build the trait.
     */
    @Override
    public LiftTrait build() {
        return element;
    }
}
