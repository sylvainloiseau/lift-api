package fr.cnrs.lacito.liftapi.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses, HasType
{

    protected final String source;

    @Getter
    protected final MultiText glosses = new MultiText();

    @Getter
    protected LiftEntry parent;

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty;
    private final ReadOnlyStringWrapper sourcePropertyWrapper;

    public LiftEtymology(LiftHeaderRangeElement type, String source) {
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

    @Override
    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
    }

    protected void setParent(LiftEntry parent) {
        this.parent = parent;
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

    public ObjectProperty<LiftHeaderRangeElement> typeProperty() {
        return typeProperty;
    }

    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyWrapper.getReadOnlyProperty();
    }

    public static LiftEtymology create(LiftHeaderRangeElement type, String source) {
        return new LiftEtymology(type, source);
    }
}
