package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.Feature;

/**
 * Builder for a {@link Feature} (a {@code <range-element>}) of a feature set.
 *
 * Usage:
 * <pre>
 *   dictionary.getComponentBuilder()
 *       .feature(dialects, "north")
 *       .withLabel("en", "Northern dialect")
 *       .build();
 * </pre>
 *
 * The feature joins its feature set on {@link #build()}; once the set is in the header,
 * the feature's label, abbreviation and description are registered as meta-language
 * texts, so their languages are declared in the dictionary.
 */
public class FeatureBuilder
    extends AbstractLiftElementBuilder<Feature, LiftHeader>
{

    public FeatureBuilder(
        LiftDictionary dictionary,
        FeatureSet parent,
        String id
    ) {
        super(new Feature(id, parent), dictionary, dictionary.getHeader());
    }


    public FeatureBuilder withLabel(String lang, String text) {
        element.getLabel().add(new Form(lang, text));
        return this;
    }

    public FeatureBuilder withAbbreviation(String lang, String text) {
        element.getAbbrev().add(new Form(lang, text));
        return this;
    }

    public FeatureBuilder withDescription(String lang, String text) {
        element.getDescription().add(new Form(lang, text));
        return this;
    }

    public FeatureBuilder withGuid(String guid) {
        element.setGuid(guid);
        return this;
    }

    /**
     * The feature above this one in its set's hierarchy (the {@code parent} attribute of
     * a {@code <range-element>}).
     */
    public FeatureBuilder withSuperordinate(Feature superordinate) {
        element.setSuperordinateFeature(superordinate);
        return this;
    }

    /**
     * Build the feature, adding it to its feature set.
     *
     * @throws IllegalArgumentException if the set already has a feature of this id
     */
    @Override
    public Feature build() {
        attach(() -> element.getParent().addFeature(element));
        return element;
    }
}
