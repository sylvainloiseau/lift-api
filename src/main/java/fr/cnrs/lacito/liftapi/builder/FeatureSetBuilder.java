package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.LiftDictionaryBuilder;
import java.util.function.Consumer;

/**
 * Fluent API for creating FeatureSet.
 * 
 * The fluent API can be used either with the {@link LiftDictionaryBuilder}:
 * 
 * <pre>
 *   LiftDictionary dictionary = Builders.dictionary()
 *       .addFeatureSet(
 *           fs -> fs.withAbbreviation("en", "myabbreviation")
 *              .withDescription("en", "my description")
 *              .withHref("http://www.foo.fr")
 *              .withLabel("en", "my label")
 *              .addFeature(f -> f.withLabel("en", "foo"), "my category 1")
 *              .addFeature(f -> f.withLabel("en", "bar"), "my category 2"),
 *           "myfeatureset"
 *       )
 *       .build();
 * </pre>
 * 
 * Or after a dictionary object creation:
 * 
 * <pre>
 *      new FeatureSetBuilder(
 *          dictionary,
 *          "myfeatureset"
 *      )
 *      .addFeature(f -> f.withLabel("en", "foo"), "my category 1")
 *      .addFeature(f -> f.withLabel("en", "bar"), "my category 2")
 *      .build();
 * </pre>
 * 
 */
public class FeatureSetBuilder
    extends AbstractLiftElementBuilder<FeatureSet, LiftHeader>
{

    public FeatureSetBuilder(
        LiftDictionary dictionary,
        String featureSetId
    ) {
        super(new FeatureSet(featureSetId, dictionary.getHeader()), dictionary, dictionary.getHeader());
    }

    public FeatureSetBuilder withLabel(String lang, String text) {
        element.getLabel().add(new Form(lang, text));
        return this;
    }

    public FeatureSetBuilder withAbbreviation(String lang, String text) {
        element.getAbbrev().add(new Form(lang, text));
        return this;
    }

    public FeatureSetBuilder withDescription(String lang, String text) {
        element.getDescription().add(new Form(lang, text));
        return this;
    }

    public FeatureSetBuilder withHref(String href) {
        element.setHref(href);
        return this;
    }

    public FeatureSetBuilder withGuid(String guid) {
        element.setGuid(guid);
        return this;
    }

    /**
     * Add a feature via nested builder configuration.
     *
     * @param type the id of the feature
     */
    public FeatureSetBuilder addFeature(Consumer<FeatureBuilder> config, String type) {
        FeatureBuilder fb = new FeatureBuilder(dictionary, this.element, type);
        config.accept(fb);
        fb.build();
        return this;
    }

    /**
     * Build the feature set, adding it - with the features built into it - to the
     * header, where its texts and those of its features are registered as meta-language
     * texts.
     *
     * @throws IllegalArgumentException if the header already has a feature set of this id
     */
    @Override
    public FeatureSet build() {
        attach(() -> dictionary.getHeader().addFeatureSet(element));
        return element;
    }
}
