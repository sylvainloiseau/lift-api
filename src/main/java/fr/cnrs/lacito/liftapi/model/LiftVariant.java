package fr.cnrs.lacito.liftapi.model;

import java.util.List;
import java.util.Optional;

import fr.cnrs.lacito.liftapi.LiftDictionaryBuilder;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

/**
 * A variant form of an entry.
 * 
 * A "variant" can relate to various phenomena: morphology, allomorphie, dialectal or in free-variation... The type of phenomena this variant belongs can be set in {@link LiftVariant#setType(Feature)}.
 *
 */
public final class LiftVariant
    extends AbstractExtensibleWithField
    implements HasType, HasPronunciation, HasRelations, HasRefId
{

    protected Optional<String> refId = Optional.empty();

    private final ObjectProperty<Feature> typeProperty = new SimpleObjectProperty<>(
        this,
        "type",
        null
    );

    protected final ListProperty<LiftPronunciation> pronunciationsProperty =
        new SimpleListProperty<>(
            this,
            "pronunciations",
            FXCollections.observableArrayList()
        );
    protected final ListProperty<LiftRelation> relationsProperty =
        new SimpleListProperty<>(
            this,
            "relations",
            FXCollections.observableArrayList()
        );
    private final ObjectProperty<AbstractIdentifiable> refObjectProperty = new SimpleObjectProperty<AbstractIdentifiable>(this, "refObject", null);

    protected LiftEntry parent;

    public LiftEntry getParent() {
        return parent;
    }

    public LiftVariant() {}

    /**
     * Get the type of the variant
     * 
     * @return a {@code Feature}, belonging to a {@link FeatureSet} containing the various variant type.
     */
    @Override
    public Feature getType() {
        return typeProperty.get();
    }

    /**
     * Set the type of the variant. In order to be set, type must have been created first in the dictionary:
     * 
     * <ul>
     * <li>either during the dictionary creation, with {@link LiftDictionaryBuilder#withVariantType(String[])} :
     * <pre>
     * LiftDictionary = dictionary = LiftDictionary.makeBuilder()
     *      .withLiftVersion(LiftVersion.V0_13)
     *      .withProducer("Test Producer")
     *      .withMetaLanguages("en")
     *      .withObjectLanguages("tww")
     *      <strong>.withVariantType("allomorph", "free-variant")</strong>
     *      .build();
     * </pre>
     * </li>
     * <li>or later, using the variant type manager at {@link LiftHeader#getVariantTypeManager()}, for instance:
     * <pre>
     * dictionary.getHeader().getVariantTypeManager().addFeature("allomorph");
     * </pre>
     * </li>
     * </ul>
     * 
     * The feature can the be used with the fluid API:
     * 
     * <pre>
     * LiftVariant v = dictionary.getComponentBuilder()
     *     .variant()
     *     .withType("allomorph")
     *     // ...
     *     .build()
     * </pre>
     * 
     * @param type: a {@code Feature}, belonging to a {@link FeatureSet} containing the variant types.
     */
    @Override
    public void setType(Feature type) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.typeProperty.set(type);
    }

    public void setRefId(String refId) {
        this.refId = Optional.of(refId);
    }

    @Override
    public Optional<String> getRefId() {
        return this.refId;
    }

    @Override
    public AbstractIdentifiable getRefObject() {
        return this.refObjectProperty.get();
    }

    @Override
    public void setRefObject(AbstractIdentifiable refObject) {
        this.refObjectProperty.set(refObject);
    }

    /**
     * The {@link LiftEntry} containing this variant.
     * @param parent
     */
    protected void setParent(LiftEntry parent) {
        this.parent = parent;
    }

    /**
     * The pronunciation(s) of this variant
     */
    @Override
    public List<LiftPronunciation> getPronunciations() {
        return pronunciationsProperty.get();
    }

    /**
     * Add a pronunciation to this variant
     */
    @Override
    public void addPronunciation(LiftPronunciation pronounciation) {
        pronunciationsProperty.add(pronounciation);
        pronounciation.setParent(this);
    }

    // TODO a bug from the LIFT data model: should'nt several variants with the same lang be possible?
    // No : add several variant instead.
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
        this.relationsProperty.add(relation);
        relation.setParent(this);
    }

    public List<LiftRelation> getRelations() {
        return relationsProperty.get();
    }

    public ListProperty<LiftPronunciation> pronunciationsProperty() {
        return pronunciationsProperty;
    }

    public ListProperty<LiftRelation> relationsProperty() {
        return relationsProperty;
    }

}
