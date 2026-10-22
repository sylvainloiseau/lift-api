package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasTrait;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.TraitSpecification;
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
        super(new LiftTrait(specification(dictionary, type)), dictionary, parent);
        if (value == null) {
            throw new IllegalArgumentException("Trait value cannot be null");
        }
        withValue(value);
    }

    /**
     * What the name refers to in the header: a definition, a range, or nothing (see
     * {@link fr.cnrs.lacito.liftapi.model.LiftHeader#resolveTraitSpecification(String)}).
     * Nothing is declared: an undeclared name is not written in the header.
     */
    private static TraitSpecification specification(LiftDictionary dictionary, String type) {
        if (type == null) {
            throw new IllegalArgumentException("Trait name cannot be null");
        }
        return dictionary.getHeader().resolveTraitSpecification(type);
    }

    /**
     * Update the trait value, parsed after the data model of the trait's specification by
     * {@link LiftTrait#parseValue(TraitSpecification, String)}: a feature id the
     * feature set does not contain yet is added to it.
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
