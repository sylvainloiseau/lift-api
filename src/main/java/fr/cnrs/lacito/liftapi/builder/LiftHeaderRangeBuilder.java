package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.FeatureSet;

public class LiftHeaderRangeBuilder
    extends AbstractLiftElementBuilder<FeatureSet, LiftHeader>
{

    public LiftHeaderRangeBuilder(
        LiftDictionary dictionary,
        String rangeId
    ) {
        super(new FeatureSet(rangeId, dictionary.getHeader()), dictionary, dictionary.getHeader());
    }

    @Override
    public FeatureSet build() {
        super.register();
        //LiftHeaderRange range = new LiftHeaderRange(rangeId, header);
        // TODO the LiftHeaderRange.setParent should be performed in addRange, for consistency :
        dictionary.getHeader().addRanges(element);
        return element;
    }
}
