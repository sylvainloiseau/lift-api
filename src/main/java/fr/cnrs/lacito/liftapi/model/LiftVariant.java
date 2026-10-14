package fr.cnrs.lacito.liftapi.model;

import java.util.Optional;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A variant form of an entry.
 *
 * A "variant" can relate to various phenomena: morphology, allomorphy, dialectal or free
 * variation... LIFT gives a variant no type attribute; the kind of variant is a trait,
 * {@code variant-type} in the files FieldWorks writes, whose values come from the
 * {@code variant-type} range when the file has one.
 */
public final class LiftVariant
    extends AbstractExtensibleWithField
    implements HasPronunciation, HasRelations, HasRefId
{

    // Plain fields until a property is asked for (see AbstractExtensibleWithoutField).
    private String refId;
    private TrackedProperty<String> refIdProperty;
    private AbstractIdentifiable refObject;
    private TrackedProperty<AbstractIdentifiable> refObjectProperty;

    final ObservableList<LiftPronunciation> pronunciations = FXCollections.observableArrayList();
    private ObservableList<LiftPronunciation> pronunciationsView;
    final ObservableList<LiftRelation> relations = FXCollections.observableArrayList();
    private ObservableList<LiftRelation> relationsView;

    protected LiftEntry parent;

    public LiftVariant() {}

    public LiftEntry getParent() {
        return parent;
    }

    /**
     * Set the LIFT id this variant refers to. On a registered variant, the dictionary's
     * reference counts follow.
     *
     * @param refId the id, or {@code null} for none
     */
    public void setRefId(String refId) {
        String oldTarget = References.target(this);
        if (refIdProperty != null) {
            refIdProperty.set(refId);
        } else {
            String old = this.refId;
            this.refId = refId;
            Changes.valueChanged(this, ModelProperty.REF_ID, old, refId);
        }
        References.retarget(this, oldTarget);
    }

    @Override
    public Optional<String> getRefId() {
        return Optional.ofNullable(refIdProperty != null ? refIdProperty.get() : refId);
    }

    /** The referred LIFT id, read-only: it changes through {@link #setRefId(String)}. */
    public ReadOnlyObjectProperty<String> refIdProperty() {
        if (refIdProperty == null) {
            refIdProperty = new TrackedProperty<>(this, ModelProperty.REF_ID, refId);
            refId = null;
        }
        return refIdProperty.getReadOnlyProperty();
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return refObjectProperty != null ? refObjectProperty.get() : refObject;
    }

    /**
     * Set the entry or sense this variant refers to. On a registered variant, the
     * dictionary's reference counts follow.
     *
     * @param refObject the referred component, or {@code null} for none
     */
    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        String oldTarget = References.target(this);
        if (refObjectProperty != null) {
            refObjectProperty.set(refObject);
        } else {
            AbstractIdentifiable old = this.refObject;
            this.refObject = refObject;
            Changes.valueChanged(this, ModelProperty.REF_OBJECT, old, refObject);
        }
        References.retarget(this, oldTarget);
    }

    /** The referred component, read-only: it changes through {@link #setRefObject}. */
    public ReadOnlyObjectProperty<AbstractIdentifiable> refObjectProperty() {
        if (refObjectProperty == null) {
            refObjectProperty = new TrackedProperty<>(this, ModelProperty.REF_OBJECT, refObject);
            refObject = null;
        }
        return refObjectProperty.getReadOnlyProperty();
    }

    /**
     * Protected: this method is called by the parent when it adopts this variant.
     *
     * @param parent the {@link LiftEntry} containing this variant
     */
    protected void setParent(LiftEntry parent) {
        this.parent = parent;
    }

    /**
     * The pronunciation(s) of this variant, read-only.
     */
    @Override
    public ObservableList<LiftPronunciation> getPronunciations() {
        if (pronunciationsView == null) pronunciationsView = FXCollections.unmodifiableObservableList(pronunciations);
        return pronunciationsView;
    }

    @Override
    public void addPronunciation(LiftPronunciation pronounciation) {
        addPronunciation(pronunciations.size(), pronounciation);
    }

    @Override
    public void addPronunciation(int index, LiftPronunciation pronounciation) {
        pronunciations.add(index, pronounciation);
        pronounciation.setParent(this);
        adopted(pronounciation);
    }

    /**
     * The transcription of the variant, with an indication of the language-writting system(s).
     *
     * @return a linguistic transcription.
     */
    public MultiText getForms() {
        return getMainMultiText();
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

    @Override
    public ObservableList<LiftRelation> getRelations() {
        if (relationsView == null) relationsView = FXCollections.unmodifiableObservableList(relations);
        return relationsView;
    }

    /** @deprecated use {@link #getPronunciations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftPronunciation> pronunciationsProperty() {
        return new ReadOnlyListWrapper<>(this, "pronunciations", getPronunciations()).getReadOnlyProperty();
    }

    /** @deprecated use {@link #getRelations()}; read-only, will be removed */
    @Deprecated(forRemoval = true)
    public ReadOnlyListProperty<LiftRelation> relationsProperty() {
        return new ReadOnlyListWrapper<>(this, "relations", getRelations()).getReadOnlyProperty();
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }

    // --------------------------------------------------------
    // Deleting sub-components
    // --------------------------------------------------------

    /**
     * Remove a pronunciation from this variant, unregistering it if this variant
     * belongs to a dictionary.
     *
     * @param pronounciation a pronunciation of this variant
     */
    @Override
    public void deletePronunciation(LiftPronunciation pronounciation) {
        requireChild(pronounciation, pronunciations.contains(pronounciation));
        removeChild(pronounciation);
    }

    /**
     * Remove a relation from this variant, unregistering it if this variant belongs to
     * a dictionary.
     *
     * @param relation a relation of this variant
     */
    @Override
    public void deleteRelation(LiftRelation relation) {
        requireChild(relation, relations.contains(relation));
        removeChild(relation);
    }

    @Override
    public void retypeRelation(LiftRelation relation, Feature type) {
        requireChild(relation, relations.contains(relation));
        relation.assignType(type);
    }
}
