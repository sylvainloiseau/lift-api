package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public final class LiftVariant
    extends AbstractExtensibleWithField
    implements HasType, HasPronunciation, HasRelations, HasRefId
{

    protected Optional<String> refId = Optional.empty();

    private final ObjectProperty<LiftHeaderRangeElement> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    protected final ListProperty<LiftPronunciation> pronunciationsProperty =
        new SimpleListProperty<>(
            this,
            "pronunciations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftRelation> relationsProperty =
        new SimpleListProperty<>(
            this,
            "relations",
            FXCollections.observableArrayList()
        );
    private final ObjectProperty<AbstractIdentifiable> refObjectProperty = new SimpleObjectProperty<AbstractIdentifiable>(this, "refObject", null);

    protected LiftEntry parent;

    public LiftEntry getParent() {
        return parent;
    }

    public LiftVariant() {}

    @Override
    public LiftHeaderRangeElement getType() {
        return typeProperty.get();
    }

    public void setType(LiftHeaderRangeElement type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
    }

    @Override
    public Optional<String> getRefId() {
        return this.refId;
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return this.refObjectProperty.get();
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        this.refObjectProperty.set(refObject);
    }

    protected void setParent(LiftEntry parent) {
        this.parent = parent;
    }

    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronunciationsProperty.get();
    }

    @Override
    public void addPronunciation(LiftPronunciation pronounciation) {
        pronunciationsProperty.add(pronounciation);
        pronounciation.setParent(this);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relationsProperty.add(relation);
        relation.setParent(this);
    }

    public List<LiftRelation> getRelations() {
        return relationsProperty.get();
    }

    public ListProperty<LiftPronunciation> pronunciationsProperty() {
        return pronunciationsProperty;
    }

    public ListProperty<LiftRelation> relationsProperty() {
        return relationsProperty;
    }

    public static LiftVariant create() {
        return new LiftVariant();
    }
}
