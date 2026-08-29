package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasTrait;
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
public class TraitBuilder extends AbstractLiftElementBuilder<LiftTrait, HasTrait> {

    /**
     * Create a trait builder with the given name and value.
     * @param dictionary the lift dictionary
     * @param parent the parent element
     * @param type the trait type
     * @param value the trait value
     */
    protected TraitBuilder(LiftDictionary dictionary, HasTrait parent, String type, String value) {
        super(new LiftTrait(dictionary.getHeader().getOrCreateTraitsDefinitions(type)), dictionary, parent);
        if (type == null || value == null) {
            throw new IllegalArgumentException("Trait name and value cannot be null");
        }
        element.setValue(value);
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
        AnnotationBuilder ab = new AnnotationBuilder(dictionary, element);
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
            element.getAnnotations().add(new AnnotationBuilder(dictionary, element, name).build());
        } else {
            element.getAnnotations().add(new AnnotationBuilder(dictionary, element, name).withValue(value).build());
        }
        return this;
    }

    /**
     * Build the trait.
     */
    @Override
    public LiftTrait build() {
        super.register();
        return element;
    }
}
