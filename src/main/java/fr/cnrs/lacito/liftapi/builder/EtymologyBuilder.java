package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import java.util.function.Consumer;

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
public class EtymologyBuilder extends AbstractLiftElementBuilder<LiftEtymology> {

    /**
     * Create an etymology builder with the given type and source.
     */
    public EtymologyBuilder(String type, String source) {
        if (type == null || source == null) {
            throw new IllegalArgumentException("Etymology type and source cannot be null");
        }
        this.element = LiftEtymology.create(type, source);
    }

    /**
     * Create an etymology builder (requires type and source to be set later).
     */
    public EtymologyBuilder() {
        this("unknown", "unknown");
    }

    /**
     * Set the etymology ID.
     */
    @Override
    public EtymologyBuilder withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Set the etymology GUID.
     */
    @Override
    public EtymologyBuilder withGuid(String guid) {
        super.withGuid(guid);
        return this;
    }

    /**
     * Add a form (etymological form) in the specified language.
     */
    public EtymologyBuilder addForm(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
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
            throw new IllegalArgumentException("Language and text cannot be null");
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

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public EtymologyBuilder addNote(String type, String language, String text) {
        super.addNote(type, language, text);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     */
    @Override
    public EtymologyBuilder addNote(Consumer<NoteBuilder> config) {
        super.addNote(config);
        return this;
    }

    /**
     * Add a trait.
     */
    @Override
    public EtymologyBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    /**
     * Add a field.
     */
    @Override
    public EtymologyBuilder addField(String name, String language, String text) {
        super.addField(name, language, text);
        return this;
    }

    /**
     * Build the etymology.
     */
    @Override
    public LiftEtymology build() {
        return element;
    }
}
