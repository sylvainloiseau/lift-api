package fr.cnrs.lacito.liftapi.model;

import lombok.Getter;
import lombok.Setter;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses {

    @Getter protected final String type;
    @Getter protected final String source;
    @Getter protected final MultiText glosses = new MultiText();
    @Getter @Setter protected LiftEntry parent;
    
    protected LiftEtymology(String type, String source) {
        this.type = type;
        this.source = source;
    }

    public void addForm(Form form) {
        addToMainMultiText(form);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    @Override
    public void addGloss(Form gloss) {
        glosses.add(gloss);
    }

    @Override
    public MultiText getGloss() {
        return glosses;
    }

}
