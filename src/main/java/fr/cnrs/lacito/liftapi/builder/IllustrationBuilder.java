package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftSense;

/**
 * Builder for the {@link LiftIllustration} of a sense (an {@code <illustration>}: a
 * reference to a picture, with an optional label).
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .illustration(sense, "pictures/house.jpg")
 *       .withLabel("en", "a house")
 *       .build();
 * </pre>
 */
public final class IllustrationBuilder extends AbstractLiftElementBuilder<LiftIllustration, LiftSense> {

    /**
     * @param href the file referred to, required by the LIFT schema
     * @throws IllegalArgumentException if {@code href} is null
     */
    protected IllustrationBuilder(LiftDictionary dictionary, LiftSense parent, String href) {
        super(new LiftIllustration(MediaBuilder.requireHref(href)), dictionary, parent);
    }

    /**
     * Add a label in the specified language.
     */
    public IllustrationBuilder withLabel(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getLabel().add(new Form(language, text));
        return this;
    }

    /**
     * Add a label.
     */
    public IllustrationBuilder withLabel(Form label) {
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        element.getLabel().add(label);
        return this;
    }

    /**
     * Build the illustration, adding it to its sense.
     */
    @Override
    public LiftIllustration build() {
        super.attach();
        return element;
    }
}
