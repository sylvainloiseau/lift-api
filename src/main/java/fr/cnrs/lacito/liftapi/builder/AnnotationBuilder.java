package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.LiftAnnotation;

/**
 * Builder for creating LiftAnnotation instances with a fluent API.
 * 
 * Usage:
 * <pre>
 *   LiftAnnotation annotation = Builders.annotation()
 *       .withName("verified")
 *       .withValue("true")
 *       .withWho("john@example.com")
 *       .withWhen("2024-01-15")
 *       .build();
 * </pre>
 */
public class AnnotationBuilder extends AbstractLiftElementBuilder<LiftAnnotation> {

    /**
     * Create an annotation builder.
     */
    public AnnotationBuilder() {
        this.element = LiftAnnotation.create("default");
    }

    /**
     * Create an annotation builder with the given name.
     */
    public AnnotationBuilder(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Annotation name cannot be null");
        }
        this.element = LiftAnnotation.create(name);
    }

    /**
     * Set the annotation ID.
     */
    @Override
    public AnnotationBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the annotation GUID.
     */
    @Override
    public AnnotationBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Set the annotation name.
     */
    public AnnotationBuilder withName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Annotation name cannot be null");
        }
        // Name is immutable in LiftAnnotation, so we need to create a new one
        this.element = LiftAnnotation.create(name);
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
        return element;
    }

    /**
     * Static helper to create an annotation quickly.
     */
    public static LiftAnnotation createAnnotation(String name) {
        return LiftAnnotation.create(name);
    }

    /**
     * Static helper to create an annotation with name and value quickly.
     */
    public static LiftAnnotation createAnnotation(String name, String value) {
        return LiftAnnotation.create(name, value);
    }
}
