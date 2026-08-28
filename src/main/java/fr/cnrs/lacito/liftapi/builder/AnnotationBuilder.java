package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.Feature;

/**
 * Builder for creating LiftAnnotation instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftAnnotation annotation = Builders.annotation("name")
 *       .withValue("true")
 *       .withWho("john@example.com")
 *       .withWhen("2024-01-15")
 *       .build();
 * </pre>
 */
public class AnnotationBuilder extends AbstractLiftElementBuilder<LiftAnnotation, HasAnnotation> {

    /**
     * Create an annotation builder.
     */
    protected AnnotationBuilder(LiftDictionary dictionary, HasAnnotation parent) {
        this(dictionary, parent, "default");
    }

    /**
     * Create an annotation builder with the given name.
     */
    protected AnnotationBuilder(LiftDictionary dictionary, HasAnnotation parent, String name) {
        super(new LiftAnnotation(), dictionary, parent);
        this.withType(name);
    }

    public AnnotationBuilder withType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Type cannot be null or empty.");
        Feature f = dictionary.getHeader().getAnnotationTypeManager().getFeature(type);
        super.withType(f);
        return this;
    }

    /**
     * Set the annotation value.
     */
    public AnnotationBuilder withValue(String value) {
        if (value != null) {
            element.setValue(value);
        }
        return this;
    }

    /**
     * Set who added this annotation.
     */
    public AnnotationBuilder withWho(String who) {
        if (who != null) {
            element.setWho(who);
        }
        return this;
    }

    /**
     * Set when this annotation was added.
     */
    public AnnotationBuilder withWhen(String when) {
        if (when != null) {
            element.setWhen(when);
        }
        return this;
    }

    /**
     * Build the annotation.
     */
    @Override
    public LiftAnnotation build() {
        super.register();
        return element;
    }
}
