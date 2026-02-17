package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import lombok.Getter;
import lombok.Setter;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses {

    protected final String type;
    protected final String source;
    @Getter protected final MultiText glosses = new MultiText();
    @Getter @Setter protected LiftEntry parent;

    private final ReadOnlyStringWrapper typePropertyWrapper;
    private final ReadOnlyStringWrapper sourcePropertyWrapper;
    
    protected LiftEtymology(String type, String source) {
        this.type = type;
        this.source = source;
        this.typePropertyWrapper = new ReadOnlyStringWrapper(this, "type", type);
        this.sourcePropertyWrapper = new ReadOnlyStringWrapper(this, "source", source);
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
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

    public ReadOnlyStringProperty typeProperty() {
        return typePropertyWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyWrapper.getReadOnlyProperty();
    }
}
