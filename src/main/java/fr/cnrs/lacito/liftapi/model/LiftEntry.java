package fr.cnrs.lacito.liftapi.model;

import fr.cnrs.lacito.liftapi.LiftDictionary;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

/**
 * Represents a Lift dictionary entry, the top-level component of a Lift dictionary.
 *
 */
public final class LiftEntry
    extends AbstractIdentifiable
    implements HasPronunciation, HasRelations, HasSense
{

    protected Optional<String> order = Optional.empty();

    protected Optional<String> dateDeleted = Optional.empty();

    protected final MultiText citations = new MultiText(this);

    protected final ListProperty<LiftPronunciation> pronunciationsProperty =
        new SimpleListProperty<>(
            this,
            "pronunciations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftVariant> variantsProperty =
        new SimpleListProperty<>(
            this,
            "variants",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftSense> sensesProperty =
        new SimpleListProperty<>(
            this,
            "senses",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftRelation> relationsProperty =
        new SimpleListProperty<>(
            this,
            "relations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftEtymology> etymologiesProperty =
        new SimpleListProperty<>(
            this,
            "etymologies",
            FXCollections.observableArrayList()
        );

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
     * <b>Not public API.</b> It is called by {@code LiftDictionaryRegistry} when the
     * entry is registered, and with {@code null} when it is unregistered; calling it
     * from anywhere else makes every component under this entry lie about the
     * dictionary it is in. It is {@code public} only because the registry lives in
     * another package - the same reason {@link AbstractLiftRoot#setUUID(java.util.UUID)}
     * is.
     *
     * @param dictionary the owning dictionary, or {@code null} when unregistering
     */
    public void setOwningDictionary(LiftDictionary dictionary) {
        this.owningDictionary = dictionary;
    }

    public MultiText getCitations() {
        return citations;
    }

    public Optional<String> getDateDeleted() {
        return dateDeleted;
    }

    public Optional<String> getOrder() {
        return order;
    }

    public void setDateDeleted(String date) {
        dateDeleted = Optional.of(date);
    }

    protected void addCitation(Form citation) {
        this.citations.add(citation);
    }

    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronunciationsProperty.get();
    }

    public List<LiftVariant> getVariants() {
        return variantsProperty.get();
    }

    public List<LiftSense> getSenses() {
        return sensesProperty.get();
    }

    public List<LiftRelation> getRelations() {
        return relationsProperty.get();
    }

    public List<LiftEtymology> getEtymologies() {
        return etymologiesProperty.get();
    }

    @Override
    public void addPronunciation(LiftPronunciation pronunciation) {
        this.pronunciationsProperty.add(pronunciation);
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
        this.variantsProperty.add(variant);
        variant.setParent(this);
        adopted(variant);
    }

    public void addSense(LiftSense sense) {
        this.sensesProperty.add(sense);
        sense.setParent(this);
        adopted(sense);
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relationsProperty.add(relation);
        relation.setParent(this);
        adopted(relation);
    }

    public void addEtymology(LiftEtymology etymology) {
        this.etymologiesProperty.add(etymology);
        etymology.setParent(this);
        adopted(etymology);
    }

    public ListProperty<LiftPronunciation> pronunciationsProperty() {
        return pronunciationsProperty;
    }

    public ListProperty<LiftVariant> variantsProperty() {
        return variantsProperty;
    }

    public ListProperty<LiftSense> sensesProperty() {
        return sensesProperty;
    }

    public ListProperty<LiftRelation> relationsProperty() {
        return relationsProperty;
    }

    public ListProperty<LiftEtymology> etymologiesProperty() {
        return etymologiesProperty;
    }

    public static LiftEntry create() {
        return new LiftEntry();
    }

    public void setOrder(String order2) {
        this.order = Optional.of(order2);
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
        this.sensesProperty.add(index, sense);
        sense.setParent(this);
        adopted(sense);
    }

    /**
     * Remove a pronunciation from this entry, unregistering it if this entry belongs to
     * a dictionary.
     *
     * @param pronunciation a pronunciation of this entry
     */
    @Override
    public void deletePronunciation(LiftPronunciation pronunciation) {
        requireChild(pronunciation, pronunciationsProperty.contains(pronunciation));
        orphaned(pronunciation);
        pronunciation.detach();
    }

    /**
     * Remove a variant from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param variant a variant of this entry
     */
    public void deleteVariant(LiftVariant variant) {
        requireChild(variant, variantsProperty.contains(variant));
        orphaned(variant);
        variant.detach();
    }

    /**
     * Remove a sense from this entry, unregistering it - and everything under it - if
     * this entry belongs to a dictionary.
     *
     * @param sense a sense of this entry
     */
    @Override
    public void deleteSense(LiftSense sense) {
        requireChild(sense, sensesProperty.contains(sense));
        orphaned(sense);
        sense.detach();
    }

    /**
     * Remove a relation from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param relation a relation of this entry
     */
    @Override
    public void deleteRelation(LiftRelation relation) {
        requireChild(relation, relationsProperty.contains(relation));
        orphaned(relation);
        relation.detach();
    }

    /**
     * Remove an etymology from this entry, unregistering it if this entry belongs to a
     * dictionary.
     *
     * @param etymology an etymology of this entry
     */
    public void deleteEtymology(LiftEtymology etymology) {
        requireChild(etymology, etymologiesProperty.contains(etymology));
        orphaned(etymology);
        etymology.detach();
    }
}
