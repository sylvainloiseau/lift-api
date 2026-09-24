package fr.cnrs.lacito.liftapi.builder;

import java.util.function.Consumer;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.Feature;

/**
 * Builder for creating LiftEtymology instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftEtymology etymology = Builders.etymology("from Latin", "Latin")
 *       .addForm("la", "original")
 *       .addGloss("en", "A description of the etymology")
 *       .build();
 * </pre>
 */
public class EtymologyBuilder
    extends AbstractLiftElementWithFieldBuilder<LiftEtymology, LiftEntry>
{

    /**
     * Create an etymology builder with the given type and source.
     */
    protected EtymologyBuilder(
        LiftDictionary dictionary,
        LiftEntry parent,
        String type,
        String source
    ) {
        super(LiftEtymology.create(null, null), dictionary, parent);

        if (type == null) {
            throw new IllegalArgumentException("Etymology type cannot be null");
        }
        if (!dictionary.getHeader().getEtymologyTypeManager().hasFeature(type)) {
            dictionary.getHeader().getEtymologyTypeManager().addFeature(type);
        }
        Feature e = dictionary.getHeader().getEtymologyTypeManager().getFeature(type);

        this.element.setType(e);
        this.element.setSource(source);

    }

    /**
     * Add a form (etymological form) in the specified language.
     */
    public EtymologyBuilder addForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException(
                "Language and text cannot be null"
            );
        }
        element.addForm(new Form(language, text));
        return this;
    }

    /**
     * Add a form.
     */
    public EtymologyBuilder addForm(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Form cannot be null");
        }
        element.addForm(form);
        return this;
    }

    /**
     * Add a gloss (description) in the specified language.
     */
    public EtymologyBuilder addGloss(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException(
                "Language and text cannot be null"
            );
        }
        element.addGloss(new Form(language, text));
        return this;
    }

    /**
     * Add a gloss.
     */
    public EtymologyBuilder addGloss(Form gloss) {
        if (gloss == null) {
            throw new IllegalArgumentException("Gloss cannot be null");
        }
        element.addGloss(gloss);
        return this;
    }

    public EtymologyBuilder withType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Type cannot be null or empty.");
        Feature f = dictionary.getHeader().getEtymologyTypeManager().getFeature(type);
        super.withType(f);
        return this;
    }

    // Override WithField so that the correct type is returned
    
    @Override
    public EtymologyBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    @Override
    public EtymologyBuilder addField(String name, Consumer<FieldBuilder> config) {
        super.addField(name, config);
        return this;
    }

    // Override Trait And Annotation builder in order to return the correct type

    @Override
    public EtymologyBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public EtymologyBuilder addTrait(
        String name,
        String value,
        Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    @Override
    public EtymologyBuilder addAnnotation(
        String name,
        String value
    ) {
        super.addAnnotation(name, value);
        return this;
    }

    /**
     * Build the etymology.
     */
    @Override
    public LiftEtymology build() {
        super.attach();
        return element;
    }
}
