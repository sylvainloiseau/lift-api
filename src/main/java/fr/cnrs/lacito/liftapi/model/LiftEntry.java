package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionary;
import fr.cnrs.lacito.liftapi.change.ModelProperty;

import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a Lift dictionary entry, the top-level component of a Lift dictionary.
 *
 */
public final class LiftEntry
    extends AbstractIdentifiable
    implements HasPronunciation, HasRelations, HasSense
{

    // Plain fields until a property is asked for (see AbstractExtensibleWithoutField).
    private String order;
    private TrackedProperty<String> orderProperty;
    private String dateDeleted;
    private TrackedProperty<String> dateDeletedProperty;

    protected final MultiText citations = new MultiText(this);

    final ObservableList<LiftPronunciation> pronunciations = FXCollections.observableArrayList();
    private ObservableList<LiftPronunciation> pronunciationsView;
    final ObservableList<LiftVariant> variants = FXCollections.observableArrayList();
    private ObservableList<LiftVariant> variantsView;
    final ObservableList<LiftSense> senses = FXCollections.observableArrayList();
    private ObservableList<LiftSense> sensesView;
    final ObservableList<LiftRelation> relations = FXCollections.observableArrayList();
    private ObservableList<LiftRelation> relationsView;
    final ObservableList<LiftEtymology> etymologies = FXCollections.observableArrayList();
    private ObservableList<LiftEtymology> etymologiesView;

    /**
     * The dictionary this entry is registered in, or {@code null} while it is detached.
     *
     * A lexical entry is the root of a component subtree, so this single reference is
     * what every component under it resolves
     * {@link AbstractLiftRoot#getOwningDictionary()} against. It is maintained by the
     * registry at (un)registration time and by nothing else.
     */
    private LiftDictionary owningDictionary;

    public LiftEntry() {}

    /**
     * The dictionary holding this entry, for
     * {@link AbstractLiftRoot#getOwningDictionary()} to return.
     *
     * Package-private: the public accessor is the inherited
     * {@code getOwningDictionary()}, which works for every kind of component.
     */
    LiftDictionary owningDictionary() {
        return owningDictionary;
    }

    /**
     * Record which dictionary this entry belongs to.
     *
     * Called by the dictionary's mutator when the entry is registered, and with
     * {@code null} when it is unregistered, through
     * {@link fr.cnrs.lacito.liftapi.internal.ModelInternals}: calling it from anywhere
     * else would make every component under this entry lie about the dictionary it is
     * in.
     *
     * @param dictionary the owning dictionary, or {@code null} when unregistering
     */
    void setOwningDictionary(LiftDictionary dictionary) {
        this.owningDictionary = dictionary;
    }

    public MultiText getCitations() {
        return citations;
    }

    public Optional<String> getDateDeleted() {
        return Optional.ofNullable(dateDeletedProperty != null ? dateDeletedProperty.get() : dateDeleted);
    }

    public Optional<String> getOrder() {
        return Optional.ofNullable(orderProperty != null ? orderProperty.get() : order);
    }

    /**
     * @param date the deletion date, or {@code null} for none
     */
    public void setDateDeleted(String date) {
        if (dateDeletedProperty != null) {
            dateDeletedProperty.set(date);
        } else {
            String old = dateDeleted;
            dateDeleted = date;
            Changes.valueChanged(this, ModelProperty.DATE_DELETED, old, date);
        }
    }

    /**
     * @param order the {@code order} attribute, or {@code null} for none
     */
    public void setOrder(String order) {
        if (orderProperty != null) {
            orderProperty.set(order);
        } else {
            String old = this.order;
            this.order = order;
            Changes.valueChanged(this, ModelProperty.ENTRY_ORDER, old, order);
        }
    }

    /** The deletion date; {@code null} when there is none. */
    public ObjectProperty<String> dateDeletedProperty() {
        if (dateDeletedProperty == null) {
            dateDeletedProperty = new TrackedProperty<>(this, ModelProperty.DATE_DELETED, dateDeleted);
            dateDeleted = null;
        }
        return dateDeletedProperty;
    }

    /** The {@code order} attribute; {@code null} when there is none. */
    public ObjectProperty<String> orderProperty() {
        if (orderProperty == null) {
            orderProperty = new TrackedProperty<>(this, ModelProperty.ENTRY_ORDER, order);
            order = null;
        }
        return orderProperty;
    }

    protected void addCitation(Form citation) {
        this.citations.add(citation);
    }

    // Read-only views ----------------------------------------------------------------

    @Override
    public ObservableList<LiftPronunciation> getPronunciations() {
        if (pronunciationsView == null) pronunciationsView = FXCollections.unmodifiableObservableList(pronunciations);
        return pronunciationsView;
    }

    public ObservableList<LiftVariant> getVariants() {
        if (variantsView == null) variantsView = FXCollections.unmodifiableObservableList(variants);
        return variantsView;
    }

    @Override
    public ObservableList<LiftSense> getSenses() {
        if (sensesView == null) sensesView = FXCollections.unmodifiableObservableList(senses);
        return sensesView;
    }

    @Override
    public ObservableList<LiftRelation> getRelations() {
        if (relationsView == null) relationsView = FXCollections.unmodifiableObservableList(relations);
        return relationsView;
    }

    public ObservableList<LiftEtymology> getEtymologies() {
        if (etymologiesView == null) etymologiesView = FXCollections.unmodifiableObservableList(etymologies);
        return etymologiesView;
    }

    // Adding sub-components ----------------------------------------------------------

    @Override
    public void addPronunciation(LiftPronunciation pronunciation) {
        addPronunciation(pronunciations.size(), pronunciation);
    }

    @Override
    public void addPronunciation(int index, LiftPronunciation pronunciation) {
        pronunciations.add(index, pronunciation);
        pronunciation.setParent(this);
        adopted(pronunciation);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    public void addForm(Form form) {
        addToMainMultiText(form);
    }

    public void addVariant(LiftVariant variant) {
        addVariant(variants.size(), variant);
    }

    /**
     * Add a variant at a given position among this entry's variants.
     *
     * @param index where to insert the variant
     * @param variant the variant to add
     */
    public void addVariant(int index, LiftVariant variant) {
        variants.add(index, variant);
        variant.setParent(this);
        adopted(variant);
    }

    @Override
    public void addSense(LiftSense sense) {
        addSense(senses.size(), sense);
    }

    /**
     * Add a sense at a given position among this entry's senses.
     *
     * Same contract as {@link #addSense(LiftSense)}; the position matters when undoing
     * a deletion, where the sense has to go back where it was.
     *
     * @param index where to insert the sense
     * @param sense the sense to add
     */
    @Override
    public void addSense(int index, LiftSense sense) {
        senses.add(index, sense);
        sense.setParent(this);
        adopted(sense);
    }

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

    public void addEtymology(LiftEtymology etymology) {
        addEtymology(etymologies.size(), etymology);
    }

    /**
     * Add an etymology at a given position among this entry's etymologies.
     *
     * @param index where to insert the etymology
     * @param etymology the etymology to add
     */
    public void addEtymology(int index, LiftEtymology etymology) {
        etymologies.add(index, etymology);
        etymology.setParent(this);
        adopted(etymology);
    }

    // Deprecated collection properties -----------------------------------------------

    /** @deprecated use {@link #getPronunciations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftPronunciation> pronunciationsProperty() {
        return new ReadOnlyListWrapper<>(this, "pronunciations", getPronunciations()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getVariants()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftVariant> variantsProperty() {
        return new ReadOnlyListWrapper<>(this, "variants", getVariants()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getSenses()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftSense> sensesProperty() {
        return new ReadOnlyListWrapper<>(this, "senses", getSenses()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getRelations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftRelation> relationsProperty() {
        return new ReadOnlyListWrapper<>(this, "relations", getRelations()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getEtymologies()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftEtymology> etymologiesProperty() {
        return new ReadOnlyListWrapper<>(this, "etymologies", getEtymologies()).getReadOnlyProperty();
    }

    public static LiftEntry create() {
        return new LiftEntry();
    }

    /**
     * A lexical entry is a root: its "parent" is the dictionary itself, reached
     * through {@link #getOwningDictionary()} rather than through this chain.
     *
     * @return always {@code null}
     */
    @Override
    public AbstractLiftRoot getParentNode() {
        return null;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a pronunciation from this entry, unregistering it if this entry belongs to
     * a dictionary.
     *
     * @param pronunciation a pronunciation of this entry
     */
    @Override
    public void deletePronunciation(LiftPronunciation pronunciation) {
        requireChild(pronunciation, pronunciations.contains(pronunciation));
        removeChild(pronunciation);
    }

    /**
     * Remove a variant from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param variant a variant of this entry
     */
    public void deleteVariant(LiftVariant variant) {
        requireChild(variant, variants.contains(variant));
        removeChild(variant);
    }

    /**
     * Remove a sense from this entry, unregistering it - and everything under it - if
     * this entry belongs to a dictionary.
     *
     * @param sense a sense of this entry
     */
    @Override
    public void deleteSense(LiftSense sense) {
        requireChild(sense, senses.contains(sense));
        removeChild(sense);
    }

    /**
     * Remove a relation from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param relation a relation of this entry
     */
    @Override
    public void deleteRelation(LiftRelation relation) {
        requireChild(relation, relations.contains(relation));
        removeChild(relation);
    }

    /**
     * Remove an etymology from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param etymology an etymology of this entry
     */
    public void deleteEtymology(LiftEtymology etymology) {
        requireChild(etymology, etymologies.contains(etymology));
        removeChild(etymology);
    }

    // --------------------------------------------------------
    // Changing the type of sub-components
    // --------------------------------------------------------

    /**
     * Change the type of an etymology of this entry. {@link LiftEtymology#setType}
     * calls this when the etymology belongs to an entry.
     *
     * @param etymology an etymology of this entry
     * @param type its new type
     */
    public void retypeEtymology(LiftEtymology etymology, Feature type) {
        requireChild(etymology, etymologies.contains(etymology));
        etymology.assignType(type);
    }

    @Override
    public void retypeRelation(LiftRelation relation, Feature type) {
        requireChild(relation, relations.contains(relation));
        relation.assignType(type);
    }
}
