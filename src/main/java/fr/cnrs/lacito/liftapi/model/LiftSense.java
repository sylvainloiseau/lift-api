package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public final class LiftSense
    extends AbstractIdentifiable
    implements HasGlosses, HasRelations, HasSense, HasReversal
{

    protected Optional<Integer> order = Optional.empty();

    public Optional<Integer> getOrder() {
        return order;
    }

    protected Optional<GrammaticalInfo> grammaticalInfo = Optional.empty();

    public Optional<GrammaticalInfo> getGrammaticalInfo() {
        return grammaticalInfo;
    }

    protected final MultiText definition = new MultiText(this);

    public MultiText getDefinition() {
        return definition;
    }

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

    private HasSense parent;

    public HasSense getParent() {
        return parent;
    }

    private LiftEntry parentEntry;

    public LiftEntry getParentEntry() {
        return parentEntry;
    }

    public void setParentEntry(LiftEntry parent) {
        this.parentEntry = parent;
    }

    public LiftSense() {}

    @Override
    public void addGloss(Form gloss) {
        addToMainMultiText(gloss);
    }

    @Override
    public MultiText getGlosses() {
        return getMainMultiText();
    }

    /**
     * Attach an existing grammatical information to this sense.
     *
     * Like every other {@code addX}/{@code setX} on the model, this only wires the two
     * components together: registering the component in the dictionary is the builder's
     * job. Use {@code DictionaryComponentBuilderFactory.grammaticalInfo(sense, pos)} to
     * create one that is part of the dictionary.
     *
     * @param gi the grammatical information to attach
     */
    public void setGrammaticalInfo(GrammaticalInfo gi) {
        this.grammaticalInfo = Optional.of(gi);
        gi.setParent(this);
    }

    /**
     * Create a grammatical information for the given part of speech and attach it.
     *
     * The component created here is <em>not</em> registered in the dictionary, because
     * a sense has no way to reach it. Prefer
     * {@code DictionaryComponentBuilderFactory.grammaticalInfo(sense, pos)}, which
     * registers it; this overload exists for the XML reader, which registers the whole
     * subtree once an entry is complete.
     *
     * @param value the part of speech
     */
    public void setGrammaticalInfo(Feature value) {
        this.setGrammaticalInfo(GrammaticalInfo.create(value));
    }

    /** Drop the grammatical information of this sense (used by {@code detach()}). */
    protected void clearGrammaticalInfo() {
        this.grammaticalInfo = Optional.empty();
    }

    public void setParent(HasSense parent) {
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
