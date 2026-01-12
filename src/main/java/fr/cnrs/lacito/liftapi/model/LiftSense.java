package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

public final class LiftSense
    extends AbstractIdentifiable
    implements HasGlosses, HasRelations, HasSense {

    @Getter protected Optional<Integer> order = Optional.empty();
    @Getter protected Optional<GrammaticalInfo> grammaticalInfo = Optional.empty();
    @Getter protected final MultiText definition = new MultiText();
    @Getter protected final List<LiftRelation> relations = new ArrayList<>();
    @Getter protected final List<LiftExample> examples = new ArrayList<>();
    @Getter protected final List<LiftIllustration> illustrations = new ArrayList<>();
    @Getter protected final List<LiftSense> subSenses = new ArrayList<>();
    private HasSense parent;
    
    protected LiftSense() {
    }

    protected void setParent(HasSense parent) { 
        this.parent = parent;
    }

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

    @Override
    public void addRelation(LiftRelation relation) {
        this.relations.add(relation);
        relation.setParent(this);
    }

    public void addExample(LiftExample example) {
        this.examples.add(example);
        example.setParent(this);
    }

    @Override
    public void addSense(LiftSense sense) {
        subSenses.add(sense);
        sense.setParent(this);
    }

    public void addIllustration(LiftIllustration illustration) {
        illustrations.add(illustration);
        illustration.setParent(this);
    }

    public void setOrder(int order) {
        this.order = Optional.of(order);
    }
    
}
