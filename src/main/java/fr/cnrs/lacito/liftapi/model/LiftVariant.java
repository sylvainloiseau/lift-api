package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import lombok.Getter;

public final class LiftVariant
    extends AbstractExtensibleWithField
    implements HasPronunciation, HasRelations, HasRefId
{

    protected Optional<String> refId = Optional.empty();

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

    @Getter
    protected LiftEntry parent;

    public LiftVariant() {}

    @Override
    public Optional<String> getRefId() {
        return this.refId;
    }

    @Override
    public void setRefId(String refId) {
        this.refId = Optional.of(refId);
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getRefObject'"
        );
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setRefObject'"
        );
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
