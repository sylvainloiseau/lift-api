package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import lombok.Getter;
import lombok.Setter;

public final class LiftSense
    extends AbstractIdentifiable
    implements HasGlosses, HasRelations, HasSense, HasReversal
{

    @Getter
    protected Optional<Integer> order = Optional.empty();

    @Getter
    protected Optional<GrammaticalInfo> grammaticalInfo = Optional.empty();

    @Getter
    protected final MultiText definition = new MultiText();

    protected final ListProperty<LiftRelation> relationsProperty =
        new SimpleListProperty<>(
            this,
            "relations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftExample> examplesProperty =
        new SimpleListProperty<>(
            this,
            "examples",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftIllustration> illustrationsProperty =
        new SimpleListProperty<>(
            this,
            "illustrations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftSense> subSensesProperty =
        new SimpleListProperty<>(
            this,
            "subSenses",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftReversal> reversalsProperty =
        new SimpleListProperty<>(
            this,
            "reversals",
            FXCollections.observableArrayList()
        );

    @Getter
    private HasSense parent;

    @Getter
    @Setter
    private LiftEntry parentEntry;

    public LiftSense() {}

    @Override
    public void addGloss(Form gloss) {
        addToMainMultiText(gloss);
    }

    @Override
    public MultiText getGloss() {
        return getMainMultiText();
    }

    protected void setGrammaticalInfo(GrammaticalInfo gi) {
        this.grammaticalInfo = Optional.of(gi);
    }

    public void setGrammaticalInfo(String value) {
        this.setGrammaticalInfo(new GrammaticalInfo(value));
    }

    protected void setParent(HasSense parent) {
        this.parent = parent;
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relationsProperty.add(relation);
        relation.setParent(this);
    }

    public void addExample(LiftExample example) {
        this.examplesProperty.add(example);
        example.setParent(this);
    }

    @Override
    public void addSense(LiftSense sense) {
        subSensesProperty.add(sense);
        sense.setParent(this);
    }

    public void addIllustration(LiftIllustration illustration) {
        illustrationsProperty.add(illustration);
        illustration.setParent(this);
    }

    public void addReversal(LiftReversal reversal) {
        reversalsProperty.add(reversal);
        reversal.setParent(this);
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }

    public List<LiftRelation> getRelations() {
        return relationsProperty.get();
    }

    public List<LiftExample> getExamples() {
        return examplesProperty.get();
    }

    public List<LiftIllustration> getIllustrations() {
        return illustrationsProperty.get();
    }

    public List<LiftSense> getSenses() {
        return subSensesProperty.get();
    }

    //public List<LiftSense> getSubSenses() {
    //    return subSensesProperty.get();
    //}

    public ListProperty<LiftRelation> relationsProperty() {
        return relationsProperty;
    }

    public ListProperty<LiftExample> examplesProperty() {
        return examplesProperty;
    }

    public ListProperty<LiftIllustration> illustrationsProperty() {
        return illustrationsProperty;
    }

    public ListProperty<LiftSense> subSensesProperty() {
        return subSensesProperty;
    }

    public List<LiftReversal> getReversals() {
        return reversalsProperty.get();
    }

    public ListProperty<LiftReversal> reversalsProperty() {
        return reversalsProperty;
    }

    public static LiftSense create() {
        return new LiftSense();
    }
}
