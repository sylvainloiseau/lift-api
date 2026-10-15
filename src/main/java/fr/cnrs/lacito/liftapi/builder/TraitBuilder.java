package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
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
        super(new LiftTrait(definition(dictionary, type)), dictionary, parent);
        if (value == null) {
            throw new IllegalArgumentException("Trait value cannot be null");
        }
        withValue(value);
    }

    private static LiftFieldAndTraitDefinition definition(LiftDictionary dictionary, String type) {
        if (type == null) {
            throw new IllegalArgumentException("Trait name cannot be null");
        }
        return dictionary.getHeader().getOrCreateTraitsDefinitions(type);
    }

    /**
     * Update the trait value, parsed after the data model of the trait's definition by
     * {@link LiftTrait#parseValue(LiftFieldAndTraitDefinition, String)}: a feature id the
     * definition's feature set does not contain yet is added to it.
     */
    public TraitBuilder withValue(String value) {
        if (value != null) {
            element.setTypedValue(LiftTrait.parseValue(element.getSpecification(), value));
        }
        return this;
    }

    /**
     * Add an annotation via nested builder configuration.
     */
    public TraitBuilder addAnnotation(Consumer<AnnotationBuilder> config) {
        AnnotationBuilder ab = new AnnotationBuilder(dictionary, element);
        config.accept(ab);
        // build() registers the annotation and attaches it to this trait.
        ab.build();
        return this;
    }

    /**
     * Add an annotation with name and optional value.
     */
    public TraitBuilder addAnnotation(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Annotation name cannot be null");
        }
        AnnotationBuilder ab = new AnnotationBuilder(dictionary, element, name);
        if (value != null) ab.withValue(value);
        // build() registers the annotation and attaches it to this trait.
        ab.build();
        return this;
    }

    /**
     * Build the trait.
     */
    @Override
    public LiftTrait build() {
        super.attach();
        return element;
    }
}
