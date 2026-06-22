package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import lombok.Getter;
import lombok.Setter;

public final class LiftEtymology
    extends AbstractExtensibleWithField
    implements HasGlosses, HasType
{

    protected Optional<String> type = Optional.empty();
    protected final String source;

    @Getter
    protected final MultiText glosses = new MultiText();

    @Getter
    protected LiftEntry parent;

    private final ReadOnlyStringWrapper typePropertyWrapper;
    private final ReadOnlyStringWrapper sourcePropertyWrapper;

    public LiftEtymology(String type, String source) {
        this.type = Optional.of(type);
        this.source = source;
        this.typePropertyWrapper = new ReadOnlyStringWrapper(
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
    public Optional<String> getType() {
        return type;
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

    public ReadOnlyStringProperty typeProperty() {
        return typePropertyWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sourceProperty() {
        return sourcePropertyWrapper.getReadOnlyProperty();
    }

    public static LiftEtymology create(String type, String source) {
        return new LiftEtymology(type, source);
    }
}
