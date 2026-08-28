package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses, HasType
{

    protected final String source;

    protected final MultiText glosses = new MultiText(this);

    protected LiftEntry parent;

    private final ObjectProperty<Feature> typeProperty;

    private final ReadOnlyStringWrapper sourcePropertyWrapper;

    public LiftEtymology(Feature type, String source) {
        this.source = source;
        this.typeProperty = new SimpleObjectProperty<>(
            this,
            "type",
            type
        );
        this.sourcePropertyWrapper = new ReadOnlyStringWrapper(
            this,
            "source",
            source
        );
    }

    public MultiText getGlosses() {
        return glosses;
    }

    public LiftEntry getParent() {
        return parent;
    }

    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    @Override
    public void setType(Feature type) {
        // TODO should check that the type belong to the etymology-type range
        this.typeProperty.set(type);
    }

    protected void setParent(LiftEntry parent) {
        this.parent = parent;
    }

    public String getSource() {
        return sourcePropertyWrapper.get();
    }

    public void setSource(String source) {
        sourcePropertyWrapper.set(source);
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

    public ObjectProperty<Feature> typeProperty() {
        return typeProperty;
    }

    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyWrapper.getReadOnlyProperty();
    }

    public static LiftEtymology create(Feature type, String source) {
        return new LiftEtymology(type, source);
    }
}
