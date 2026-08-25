package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.HasAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;

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

    private String name;

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
        this.withName(name);
    }

    public AnnotationBuilder withName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Annotation name cannot be null");
        }
        if (!dictionary.getHeader().getAnnotationTypeManager().hasRangeElements(name)) {
            dictionary.getHeader().getAnnotationTypeManager().createRangeElement(name);
        }
        LiftHeaderRangeElement e = dictionary.getHeader().getAnnotationTypeManager().getRangeElement(name);
        element.setName(e);
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
