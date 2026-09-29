package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftTranslation;

/**
 * Builder for creating LiftTranslation instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   LiftTranslation translation = dictionary.getComponentBuilder()
 *       .translation(example, "free")
 *       .addText("fr", "J'ai trouvé le mot dans le dictionnaire")
 *       .build();
 * </pre>
 */
public final class TranslationBuilder extends AbstractLiftElementBuilder<LiftTranslation, LiftExample> {

    /**
     * Create a translation with a type, created in the header's translation-type range
     * if not declared there yet.
     */
    protected TranslationBuilder(LiftDictionary dictionary, LiftExample parent, String type) {
        this(
            dictionary,
            parent,
            dictionary.getHeader().getTranslationTypeManager().getOrCreateFeature(requireType(type))
        );
    }

    protected TranslationBuilder(LiftDictionary dictionary, LiftExample parent, Feature type) {
        super(LiftTranslation.create(type), dictionary, parent);
    }

    private static String requireType(String type) {
        if (type == null) throw new IllegalArgumentException("Translation type cannot be null");
        return type;
    }

    /**
     * Add text in the specified language.
     */
    public TranslationBuilder addText(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getTranslation().add(new Form(language, text));
        return this;
    }

    /**
     * Add text.
     */
    public TranslationBuilder addText(Form text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        element.getTranslation().add(text);
        return this;
    }

    /**
     * Build the translation.
     *
     * @throws fr.cnrs.lacito.liftapi.model.DuplicateTypeException if the example
     *         already has a translation of this type
     */
    @Override
    public LiftTranslation build() {
        super.attach();
        return element;
    }
}
