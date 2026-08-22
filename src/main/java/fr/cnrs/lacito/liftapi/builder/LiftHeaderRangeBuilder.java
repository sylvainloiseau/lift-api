package fr.cnrs.lacito.liftapi.builder;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftHeaderRange;

public class LiftHeaderRangeBuilder
    extends AbstractLiftElementBuilder<LiftHeaderRange, LiftHeader>
{

    public LiftHeaderRangeBuilder(
        LiftDictionary dictionary,
        String rangeId
    ) {
        super(new LiftHeaderRange(rangeId, dictionary.getHeader()), dictionary, dictionary.getHeader());
    }

    @Override
    public LiftHeaderRange build() {
        super.register();
        //LiftHeaderRange range = new LiftHeaderRange(rangeId, header);
        // TODO the LiftHeaderRange.setParent should be performed in addRange, for consistency :
        dictionary.getHeader().addRanges(element);
        return element;
    }
}
