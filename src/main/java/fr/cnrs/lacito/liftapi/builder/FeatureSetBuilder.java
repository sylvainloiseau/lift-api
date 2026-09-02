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

    public FeatureSetBuilder witGuid(String guid) {
        element.setGuid(guid);
        return this;
    }

    /**
     * Add a note via nested builder configuration.
     *
     * @throws IllegalArgumentException if the element built is not an instance of HasNote.
     */
    public FeatureSetBuilder addFeature(Consumer<FeatureBuilder> config, String type) {
        FeatureBuilder fb = new FeatureBuilder(dictionary, this.element, type);
        config.accept(fb);
        fb.build();
        return this;
    }

    @Override
    public FeatureSet build() {
        // super.register();
        
        //LiftHeaderRange range = new LiftHeaderRange(rangeId, header);
        // TODO the LiftHeaderRange.setParent should be performed in addRange, for consistency :
        dictionary.getHeader().addFeatureSet(element);
        // TODO datetime
        // element.setDateCreated(ZonedDateTime.now());
        // element.setDateModified(ZonedDateTime.now());
        return element;
    }
}
