package fr.cnrs.lacito.liftapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

public final class LiftVariant
    extends AbstractExtensibleWithField
    implements HasPronunciation, HasRelations {

    @Getter protected Optional<String> refId = Optional.empty();
    protected final List<LiftPronunciation> pronunciations = new ArrayList<>();
    @Getter protected final List<LiftRelation> relations = new ArrayList<>();
    @Setter @Getter protected LiftEntry parent;

    protected LiftVariant() {
    }

    protected void setRefId(String refId) {
        this.refId = Optional.of(refId);
    }

    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronunciations;
    }

    @Override
    public void addPronunciation(LiftPronunciation pronounciation) {
        pronunciations.add(pronounciation);
        pronounciation.setParent(this);
    }

    public MultiText getForms() {
        return getMainMultiText();
    }

    @Override
    public void addRelation(LiftRelation relation) {
        this.relations.add(relation);
        relation.setParent(this);
    }

}
