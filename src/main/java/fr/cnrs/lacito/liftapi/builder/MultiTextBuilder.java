package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.MultiText;

/**
 * Builder for creating MultiText instances with a fluent API.
 * 
 * MultiText represents a multilingual text container.
 * 
 * Usage:
 * <pre>
 *   MultiText text = Builders.multiText()
 *       .withForm("en", "hello")
 *       .withForm("fr", "bonjour")
 *       .withForm("es", "hola")
 *       .build();
 * </pre>
 */
public class MultiTextBuilder {

    private final MultiText element;

    /**
     * Create a MultiText builder.
     */
    public MultiTextBuilder() {
        this.element = new MultiText();
    }

    /**
     * Add a form in the specified language.
     */
    public MultiTextBuilder withForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.add(new Form(language, text));
        return this;
    }

    /**
     * Add a form.
     */
    public MultiTextBuilder withForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.add(form);
        return this;
    }

    /**
     * Add multiple forms.
     */
    public MultiTextBuilder withForms(Form... forms) {
        if (forms != null) {
            for (Form form : forms) {
                if (form != null) {
                    element.add(form);
                }
            }
        }
        return this;
    }

    // /**
    //  * Clear all existing forms and start fresh.
    //  */
    // public MultiTextBuilder clear() {
    //     element.clear();
    //     return this;
    // }

    /**
     * Build the MultiText.
     */
    public MultiText build() {
        return element;
    }
}
