package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.LiftSense;

/**
 * Builder for creating {@link GrammaticalInfo} instances with a fluent API.
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .grammaticalInfo(sense, "Noun")
 *       .addTrait("Noun-infl-class", "fo")
 *       .build();
 * </pre>
 *
 * A sense holds at most one grammatical information, so building a second one for the
 * same sense replaces the first; the replaced component is removed from the dictionary
 * rather than left registered but unreachable.
 */
public final class GrammaticalInfoBuilder
    extends AbstractLiftElementWithoutFieldBuilder<GrammaticalInfo, LiftSense> {

    /**
     * Create a builder for the grammatical information of {@code parent}.
     *
     * @param dictionary the dictionary the sense belongs to
     * @param parent the sense this grammatical information describes
     * @param partOfSpeech the part of speech; created in the grammatical-info range of
     *        the header if it is not declared there yet
     */
    protected GrammaticalInfoBuilder(
        LiftDictionary dictionary,
        LiftSense parent,
        String partOfSpeech
    ) {
        this(
            dictionary,
            parent,
            resolve(dictionary, partOfSpeech)
        );
    }

    /**
     * Create a builder for the grammatical information of {@code parent}.
     *
     * @param dictionary the dictionary the sense belongs to
     * @param parent the sense this grammatical information describes
     * @param partOfSpeech the part of speech, taken from the header's
     *        grammatical-info range
     */
    protected GrammaticalInfoBuilder(
        LiftDictionary dictionary,
        LiftSense parent,
        Feature partOfSpeech
    ) {
        super(GrammaticalInfo.create(partOfSpeech), dictionary, parent);
        if (parent == null) {
            throw new IllegalArgumentException(
                "Grammatical information must belong to a sense"
            );
        }
    }

    private static Feature resolve(LiftDictionary dictionary, String partOfSpeech) {
        if (partOfSpeech == null || partOfSpeech.isBlank()) {
            throw new IllegalArgumentException(
                "Part of speech cannot be null or empty"
            );
        }
        return dictionary
            .getHeader()
            .getGrammaticalInfoManager()
            .getOrCreateFeature(partOfSpeech);
    }

    // Override addTrait so that the correct type is returned.

    @Override
    public GrammaticalInfoBuilder addTrait(String name, String value) {
        super.addTrait(name, value);
        return this;
    }

    @Override
    public GrammaticalInfoBuilder addTrait(
        String name,
        String value,
        java.util.function.Consumer<TraitBuilder> config
    ) {
        super.addTrait(name, value, config);
        return this;
    }

    /**
     * Build the grammatical information, registering it in the dictionary and setting
     * it on the sense.
     */
    @Override
    public GrammaticalInfo build() {
        // A sense holds a single grammatical information. Take any previous one out of
        // the dictionary first, so that replacing it does not leave a registered
        // component nothing refers to.
        parent
            .getGrammaticalInfo()
            .filter(existing -> existing.getUUID() != null)
            .ifPresent(registry::removeFromDictionary);
        super.register();
        return element;
    }
}
