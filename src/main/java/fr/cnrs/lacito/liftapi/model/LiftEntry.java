package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import lombok.Getter;

public final class LiftEntry
    extends AbstractIdentifiable
    implements HasPronunciation, HasRelations, HasSense {

    @Getter protected Optional<String> order = Optional.empty();
    @Getter protected Optional<String> dateDeleted = Optional.empty();

    @Getter final protected MultiText citations = new MultiText();
    protected final ListProperty<LiftPronunciation> pronounciationsProperty =
            new SimpleListProperty<>(this, "pronunciations", FXCollections.observableArrayList());
    protected final ListProperty<LiftVariant> variantsProperty =
            new SimpleListProperty<>(this, "variants", FXCollections.observableArrayList());
    protected final ListProperty<LiftSense> sensesProperty =
            new SimpleListProperty<>(this, "senses", FXCollections.observableArrayList());
    protected final ListProperty<LiftRelation> relationsProperty =
            new SimpleListProperty<>(this, "relations", FXCollections.observableArrayList());
    protected final ListProperty<LiftEtymology> etymologiesProperty =
            new SimpleListProperty<>(this, "etymologies", FXCollections.observableArrayList());

    protected LiftEntry() {
    }

    /**
     * Factory method for creating new LiftEntry instances.
     * Used by builders and other factory patterns.
     */
    public static LiftEntry create() {
        return new LiftEntry();
    }

    public void setDateDeleted(String date) {
        dateDeleted = Optional.of(date);
    }

    protected void addCitation(Form citation) {
        this.citations.add(citation);
    }

    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronounciationsProperty.get();
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
        this.pronounciationsProperty.add(pronunciation);
        pronunciation.setParent(this);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    protected void addForm(Form form) {
        addToMainMultiText(form);
    }

    public void addVariant(LiftVariant variant) {
        this.variantsProperty.add(variant);
        variant.setParent(this);
    }

    public void addSense(LiftSense sense) {
        this.sensesProperty.add(sense);
        sense.setParent(this);
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relationsProperty.add(relation);
        relation.setParent(this);
    }

    public void addEtymology(LiftEtymology etymology) {
        this.etymologiesProperty.add(etymology);
        etymology.setParent(this);
    }

    public ListProperty<LiftPronunciation> pronunciationsProperty() {
        return pronounciationsProperty;
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

    public void addText(Form form) {
        addToMainMultiText(form);
    }

    // TODO : check homophoneous forms for order consistency
    public void setOrder(String order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        } else if (this.order.isEmpty()) {
            throw new IllegalStateException("Order is already set");
        } else if (Integer.parseInt(order) < 0) {
            throw new IllegalArgumentException("Order cannot be negative");
        } 
        this.order = Optional.of(order);
    }
}
