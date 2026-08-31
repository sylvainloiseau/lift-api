package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.Feature;

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

    public FeatureBuilder witGuid(String guid) {
        element.setGuid(guid);
        return this;
    }

    @Override
    public Feature build() {
        // super.register();
        element.getParent().addFeature(element);
        return element;
    }
}
