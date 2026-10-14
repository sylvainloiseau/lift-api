package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class LiftSense
    extends AbstractIdentifiable
    implements HasGlosses, HasRelations, HasSense, HasReversal
{

    // Plain field until a property is asked for (see AbstractExtensibleWithoutField).
    private Integer order;
    private TrackedProperty<Integer> orderProperty;

    /**
     * The grammatical info: a component in a single slot, so its changes are reported
     * as components added and removed. Its property, created on demand, only mirrors it.
     */
    private GrammaticalInfo grammaticalInfo;
    private ReadOnlyObjectWrapper<GrammaticalInfo> grammaticalInfoProperty;

    protected final MultiText definition = new MultiText(this);

    final ObservableList<LiftRelation> relations = FXCollections.observableArrayList();
    private ObservableList<LiftRelation> relationsView;
    final ObservableList<LiftExample> examples = FXCollections.observableArrayList();
    private ObservableList<LiftExample> examplesView;
    final ObservableList<LiftIllustration> illustrations = FXCollections.observableArrayList();
    private ObservableList<LiftIllustration> illustrationsView;
    final ObservableList<LiftSense> subSenses = FXCollections.observableArrayList();
    private ObservableList<LiftSense> subSensesView;
    final ObservableList<LiftReversal> reversals = FXCollections.observableArrayList();
    private ObservableList<LiftReversal> reversalsView;

    private HasSense parent;

    public LiftSense() {}

    public Optional<Integer> getOrder() {
        return Optional.ofNullable(orderProperty != null ? orderProperty.get() : order);
    }

    /**
     * @param order the {@code order} attribute, or {@code null} for none
     */
    public void setOrder(Integer order) {
        if (orderProperty != null) {
            orderProperty.set(order);
        } else {
            Integer old = this.order;
            this.order = order;
            Changes.valueChanged(this, ModelProperty.ORDER, old, order);
        }
    }

    /** The {@code order} attribute; {@code null} when there is none. */
    public ObjectProperty<Integer> orderProperty() {
        if (orderProperty == null) {
            orderProperty = new TrackedProperty<>(this, ModelProperty.ORDER, order);
            order = null;
        }
        return orderProperty;
    }

    public Optional<GrammaticalInfo> getGrammaticalInfo() {
        return Optional.ofNullable(grammaticalInfo);
    }

    /**
     * The grammatical info, read-only: it changes through
     * {@link #setGrammaticalInfo(GrammaticalInfo)} and {@link #deleteGrammaticalInfo()}.
     */
    public ReadOnlyObjectProperty<GrammaticalInfo> grammaticalInfoProperty() {
        if (grammaticalInfoProperty == null) {
            grammaticalInfoProperty = new ReadOnlyObjectWrapper<>(this, "grammaticalInfo", grammaticalInfo);
        }
        return grammaticalInfoProperty.getReadOnlyProperty();
    }

    private void storeGrammaticalInfo(GrammaticalInfo gi) {
        this.grammaticalInfo = gi;
        if (grammaticalInfoProperty != null) {
            grammaticalInfoProperty.set(gi);
        }
    }

    public MultiText getDefinition() {
        return definition;
    }

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

    @Override
    public void addGloss(Form gloss) {
        addToMainMultiText(gloss);
    }

    @Override
    public MultiText getGlosses() {
        return getMainMultiText();
    }

    /**
     * Attach a grammatical information to this sense, in place of the current one.
     *
     * Like every other {@code addX} on the model, this wires the two components together
     * and then registers {@code gi} in the dictionary - but only if this sense is itself
     * attached to one. A current grammatical info is deleted first (and unregistered),
     * so that nothing is left in the dictionary that the sense no longer refers to.
     *
     * @param gi the grammatical information to attach
     */
    public void setGrammaticalInfo(GrammaticalInfo gi) {
        if (gi == null) {
            deleteGrammaticalInfo();
            return;
        }
        if (gi == grammaticalInfo) {
            return;
        }
        deleteGrammaticalInfo();
        storeGrammaticalInfo(gi);
        gi.setParent(this);
        adopted(gi);
    }

    /**
     * Create a grammatical information for the given part of speech and attach it, in
     * place of the current one (see {@link #setGrammaticalInfo(GrammaticalInfo)}).
     *
     * @param value the part of speech
     */
    public void setGrammaticalInfo(Feature value) {
        this.setGrammaticalInfo(GrammaticalInfo.create(value));
    }

    /** Drop the grammatical information of this sense (used when unlinking it). */
    void clearGrammaticalInfo() {
        storeGrammaticalInfo(null);
    }

    /**
     * Protected: this method is called by the parent when it adopts this LiftSense.
     *
     * @param parent the new parent, or {@code null} when detaching this sense
     *        (see {@link AbstractLiftRoot#detach()})
     */
    protected void setParent(HasSense parent) {
        this.parent = parent;
    }

    // Read-only views ----------------------------------------------------------------

    @Override
    public ObservableList<LiftRelation> getRelations() {
        if (relationsView == null) relationsView = FXCollections.unmodifiableObservableList(relations);
        return relationsView;
    }

    public ObservableList<LiftExample> getExamples() {
        if (examplesView == null) examplesView = FXCollections.unmodifiableObservableList(examples);
        return examplesView;
    }

    public ObservableList<LiftIllustration> getIllustrations() {
        if (illustrationsView == null) illustrationsView = FXCollections.unmodifiableObservableList(illustrations);
        return illustrationsView;
    }

    @Override
    public ObservableList<LiftSense> getSenses() {
        if (subSensesView == null) subSensesView = FXCollections.unmodifiableObservableList(subSenses);
        return subSensesView;
    }

    @Override
    public ObservableList<LiftReversal> getReversals() {
        if (reversalsView == null) reversalsView = FXCollections.unmodifiableObservableList(reversals);
        return reversalsView;
    }

    // Adding sub-components ----------------------------------------------------------

    @Override
    public void addRelation(LiftRelation relation) {
        addRelation(relations.size(), relation);
    }

    @Override
    public void addRelation(int index, LiftRelation relation) {
        relations.add(index, relation);
        relation.setParent(this);
        adopted(relation);
    }

    public void addExample(LiftExample example) {
        addExample(examples.size(), example);
    }

    /**
     * Add an example at a given position.
     *
     * @param index where to insert the example
     * @param example the example to add
     */
    public void addExample(int index, LiftExample example) {
        examples.add(index, example);
        example.setParent(this);
        adopted(example);
    }

    @Override
    public void addSense(LiftSense sense) {
        addSense(subSenses.size(), sense);
    }

    /**
     * Add a subsense at a given position.
     *
     * @param index where to insert the subsense
     * @param sense the subsense to add
     */
    @Override
    public void addSense(int index, LiftSense sense) {
        subSenses.add(index, sense);
        sense.setParent(this);
        adopted(sense);
    }

    public void addIllustration(LiftIllustration illustration) {
        addIllustration(illustrations.size(), illustration);
    }

    /**
     * Add an illustration at a given position.
     *
     * @param index where to insert the illustration
     * @param illustration the illustration to add
     */
    public void addIllustration(int index, LiftIllustration illustration) {
        illustrations.add(index, illustration);
        illustration.setParent(this);
        adopted(illustration);
    }

    @Override
    public void addReversal(LiftReversal reversal) {
        addReversal(reversals.size(), reversal);
    }

    @Override
    public void addReversal(int index, LiftReversal reversal) {
        reversals.add(index, reversal);
        reversal.setParent(this);
        adopted(reversal);
    }

    // Deprecated collection properties -----------------------------------------------

    /** @deprecated use {@link #getRelations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftRelation> relationsProperty() {
        return new ReadOnlyListWrapper<>(this, "relations", getRelations()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getExamples()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftExample> examplesProperty() {
        return new ReadOnlyListWrapper<>(this, "examples", getExamples()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getIllustrations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftIllustration> illustrationsProperty() {
        return new ReadOnlyListWrapper<>(this, "illustrations", getIllustrations()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getSenses()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftSense> subSensesProperty() {
        return new ReadOnlyListWrapper<>(this, "subSenses", getSenses()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getReversals()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftReversal> reversalsProperty() {
        return new ReadOnlyListWrapper<>(this, "reversals", getReversals()).getReadOnlyProperty();
    }

    public static LiftSense create() {
        return new LiftSense();
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return (AbstractLiftRoot) parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a relation from this sense, unregistering it if this sense belongs to a
     * dictionary.
     *
     * @param relation a relation of this sense
     */
    @Override
    public void deleteRelation(LiftRelation relation) {
        requireChild(relation, relations.contains(relation));
        removeChild(relation);
    }

    /**
     * Remove an example from this sense, unregistering it - and everything under it -
     * if this sense belongs to a dictionary.
     *
     * @param example an example of this sense
     */
    public void deleteExample(LiftExample example) {
        requireChild(example, examples.contains(example));
        removeChild(example);
    }

    /**
     * Remove a subsense from this sense, unregistering it - and everything under it -
     * if this sense belongs to a dictionary.
     *
     * @param sense a subsense of this sense
     */
    @Override
    public void deleteSense(LiftSense sense) {
        requireChild(sense, subSenses.contains(sense));
        removeChild(sense);
    }

    /**
     * Remove an illustration from this sense, unregistering it if this sense belongs to
     * a dictionary.
     *
     * @param illustration an illustration of this sense
     */
    public void deleteIllustration(LiftIllustration illustration) {
        requireChild(illustration, illustrations.contains(illustration));
        removeChild(illustration);
    }

    /**
     * Remove a reversal from this sense, unregistering it - and everything under it -
     * if this sense belongs to a dictionary.
     *
     * @param reversal a reversal of this sense
     */
    @Override
    public void deleteReversal(LiftReversal reversal) {
        requireChild(reversal, reversals.contains(reversal));
        removeChild(reversal);
    }

    @Override
    public void retypeReversal(LiftReversal reversal, Feature type) {
        requireChild(reversal, reversals.contains(reversal));
        reversal.assignType(type);
    }

    @Override
    public void retypeRelation(LiftRelation relation, Feature type) {
        requireChild(relation, relations.contains(relation));
        relation.assignType(type);
    }

    /**
     * Remove this sense's grammatical information, unregistering it - and the traits it
     * carries - if this sense belongs to a dictionary.
     *
     * Does nothing if the sense has none. This is the counterpart of
     * {@link #setGrammaticalInfo(GrammaticalInfo)}, which holds at most one.
     */
    public void deleteGrammaticalInfo() {
        if (grammaticalInfo != null) {
            removeChild(grammaticalInfo);
        }
    }
}
