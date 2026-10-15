package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftPronunciation;

/**
 * Builder for the {@link LiftMedia} of a pronunciation (a {@code <media>}: a reference
 * to a sound file, with an optional label).
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .media(pronunciation, "audio/word.wav")
 *       .withLabel("en", "careful speech")
 *       .build();
 * </pre>
 */
public final class MediaBuilder extends AbstractLiftElementBuilder<LiftMedia, LiftPronunciation> {

    /**
     * @param href the file referred to, required by the LIFT schema
     * @throws IllegalArgumentException if {@code href} is null
     */
    protected MediaBuilder(LiftDictionary dictionary, LiftPronunciation parent, String href) {
        super(new LiftMedia(requireHref(href)), dictionary, parent);
    }

    static String requireHref(String href) {
        if (href == null) {
            throw new IllegalArgumentException("The LIFT schema requires an 'href' attribute here");
        }
        return href;
    }

    /**
     * Add a label in the specified language.
     */
    public MediaBuilder withLabel(String language, String text) {
        if (language == null || text == null) {
            throw new IllegalArgumentException("Language and text cannot be null");
        }
        element.getLabel().add(new Form(language, text));
        return this;
    }

    /**
     * Add a label.
     */
    public MediaBuilder withLabel(Form label) {
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        element.getLabel().add(label);
        return this;
    }

    /**
     * Build the media, adding it to its pronunciation.
     */
    @Override
    public LiftMedia build() {
        super.attach();
        return element;
    }
}
