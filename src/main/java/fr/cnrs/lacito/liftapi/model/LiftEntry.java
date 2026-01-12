package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Getter;

public final class LiftEntry
    extends AbstractIdentifiable
    implements HasPronunciation, HasRelations, HasSense {

    @Getter protected Optional<String> order = Optional.empty();
    @Getter protected Optional<String> dateDeleted = Optional.empty();

    @Getter final protected MultiText citations = new MultiText();
    final protected List<LiftPronunciation> pronounciations = new ArrayList<>();
    @Getter final protected List<LiftVariant> variants = new ArrayList<>();
    @Getter final protected List<LiftSense> senses = new ArrayList<>();
    @Getter final protected List<LiftRelation> relations = new ArrayList<>();
    @Getter final protected List<LiftEtymology> etymologies = new ArrayList<>();

    protected LiftEntry() {
    }

    public void setDateDeleted(String date) {
        dateDeleted = Optional.of(date);
    }

    protected void addCitation(Form citation) {
        this.citations.add(citation);
    }

    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronounciations;
    }

    @Override
    public void addPronunciation(LiftPronunciation pronunciation) {
        this.pronounciations.add(pronunciation);
        pronunciation.setParent(this);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    protected void addForm(Form form) {
        addToMainMultiText(form);
    }

    public void addVariant(LiftVariant variant) {
        this.variants.add(variant);
        variant.setParent(this);
    }

    public void addSense(LiftSense sense) {
        this.senses.add(sense);
        sense.setParent(this);
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relations.add(relation);
        relation.setParent(this);
    }

    protected void addEtymology(LiftEtymology etymology) {
        this.etymologies.add(etymology);
        etymology.setParent(this);
    }

}
