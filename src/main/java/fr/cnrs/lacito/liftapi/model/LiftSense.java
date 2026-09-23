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

    /**
     * The entry this sense belongs to, directly or through enclosing subsenses.
     *
     * This used to be a second, independently stored link, set by some callers and not
     * by others, so a sense could disagree with itself about which entry it was in.
     * It is now derived from the one parent chain and cannot desync.
     *
     * @return the enclosing entry, or {@code null} if this sense is not wired into one
     */
    public LiftEntry getParentEntry() {
        for (AbstractLiftRoot n = getParentNode(); n != null; n = n.getParentNode()) {
            if (n instanceof LiftEntry e) {
                return e;
            }
        }
        return null;
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
     * Like every other {@code addX}/{@code setX} on the model, this wires the two
     * components together and then registers {@code gi} in the dictionary - but only if
     * this sense is itself attached to one. On a detached sense it merely wires, and
     * registration happens later, when the subtree is attached.
     *
     * @param gi the grammatical information to attach
     */
    public void setGrammaticalInfo(GrammaticalInfo gi) {
        this.grammaticalInfo = Optional.of(gi);
        gi.setParent(this);
        adopted(gi);
    }

    /**
     * Create a grammatical information for the given part of speech and attach it.
     *
     * Note that this replaces any previous grammatical information without taking it
     * out of the dictionary, which leaves a registered component nothing refers to.
     * {@code DictionaryComponentBuilderFactory.grammaticalInfo(sense, pos)} handles
     * that; this overload exists for the XML reader, which builds each sense once.
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
        adopted(relation);
    }

    public void addExample(LiftExample example) {
        this.examplesProperty.add(example);
        example.setParent(this);
        adopted(example);
    }

    @Override
    public void addSense(LiftSense sense) {
        subSensesProperty.add(sense);
        sense.setParent(this);
        adopted(sense);
    }

    public void addIllustration(LiftIllustration illustration) {
        illustrationsProperty.add(illustration);
        illustration.setParent(this);
        adopted(illustration);
    }

    public void addReversal(LiftReversal reversal) {
        reversalsProperty.add(reversal);
        reversal.setParent(this);
        adopted(reversal);
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

    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }
}
