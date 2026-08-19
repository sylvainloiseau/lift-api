package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.LiftDictionaryRegistry;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRangeElement;

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
public class EtymologyBuilder
    extends AbstractLiftElementBuilder<LiftEtymology, LiftEntry>
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
        if (!dictionary.getHeader().getEtymologyTypeManager().hasRangeElements(type)) {
            dictionary.getHeader().getEtymologyTypeManager().createRangeElement(type);
        }
        LiftHeaderRangeElement e = dictionary.getHeader().getEtymologyTypeManager().getRangeElement(type);

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

//    /**
//     * Add a note via nested builder configuration.
//     */
//    @Override
//    public EtymologyBuilder addNote(String type, String language, String text) {
//        super.addNote(type, language, text);
//        return this;
//    }

//    /**
//     * Add a note via nested builder configuration.
//     */
//    @Override
//    public EtymologyBuilder addNote(Consumer<NoteBuilder> config, String type) {
//        super.addNote(config, type);
//        return this;
//    }
//
//    /**
//     * Add a trait.
//     */
//    @Override
//    public EtymologyBuilder addTrait(String name, String value) {
//        super.addTrait(name, value);
//        return this;
//    }

//    /**
//     * Add a field.
//     */
//    @Override
//    public EtymologyBuilder addField(
//        String name,
//        String language,
//        String text
//    ) {
//        super.addField(name, language, text);
//        return this;
//    }

    /**
     * Build the etymology.
     */
    @Override
    public LiftEtymology build() {
        super.register();
        return element;
    }
}
