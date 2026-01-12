package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;

public final class LiftHeader extends AbstractLiftRoot {

    private List<LiftHeaderFieldDefinition> fields = new ArrayList<>();
    private List<LiftHeaderRange> ranges = new ArrayList<>();

    protected LiftHeader() {
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    public List<LiftHeaderRange> getRanges() {
        return ranges;
    }

    public List<LiftHeaderFieldDefinition> getFields() {
        return fields;
    }
}
