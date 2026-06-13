package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
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
public abstract sealed class MultiTextBuilder permits MultitextMetaLanguageBuilder, MultitextObjectLanguageBuilder {

    protected MultiText element;
    protected final LiftDictionaryRegistry registry;

    /**
     * Create a MultiText builder.
     * @param registry 
     */
    protected MultiTextBuilder(LiftDictionaryRegistry registry) {
        this.registry = registry;
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

    /**
     * Build the MultiText.
     */
    public abstract MultiText build();
}
