package fr.cnrs.lacito.liftapi.change;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import fr.cnrs.lacito.liftapi.internal.ModelInternals;
import fr.cnrs.lacito.liftapi.model.AbstractExtensibleWithoutField;
import fr.cnrs.lacito.liftapi.model.AbstractIdentifiable;
import fr.cnrs.lacito.liftapi.model.Feature;
import fr.cnrs.lacito.liftapi.model.FeatureSet;
import fr.cnrs.lacito.liftapi.model.FieldSpecification;
import fr.cnrs.lacito.liftapi.model.Form;
import fr.cnrs.lacito.liftapi.model.GrammaticalInfo;
import fr.cnrs.lacito.liftapi.model.HasType;
import fr.cnrs.lacito.liftapi.model.LiftAnnotation;
import fr.cnrs.lacito.liftapi.model.LiftEntry;
import fr.cnrs.lacito.liftapi.model.LiftEtymology;
import fr.cnrs.lacito.liftapi.model.LiftExample;
import fr.cnrs.lacito.liftapi.model.LiftField;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinition;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionDataModel;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionKind;
import fr.cnrs.lacito.liftapi.model.LiftFieldAndTraitDefinitionTarget;
import fr.cnrs.lacito.liftapi.model.LiftHeader;
import fr.cnrs.lacito.liftapi.model.LiftIllustration;
import fr.cnrs.lacito.liftapi.model.LiftMedia;
import fr.cnrs.lacito.liftapi.model.LiftRelation;
import fr.cnrs.lacito.liftapi.model.LiftSense;
import fr.cnrs.lacito.liftapi.model.LiftTrait;
import fr.cnrs.lacito.liftapi.model.LiftVariant;

/// Which value of an object a {@link ValueChanged} is about.
///
/// One constant per kind of value. A consumer can switch on the constant, or use its
/// {@link #name()} for a label. Each constant knows how to set its value through the
/// public API of the model ({@code setType}, {@code changeFeatureId}, a setter...), which
/// is what makes {@link ValueChanged#revert()} keep the dictionary consistent.
///
/// @param <T> the type of the value; {@code null} stands for "no value"
public final class ModelProperty<T> {

    // Every component -----------------------------------------------------------------

    /// The creation date of a component ({@code dateCreated}).
    public static final ModelProperty<String> DATE_CREATED = new ModelProperty<>(
        "dateCreated", (o, v) -> ((AbstractExtensibleWithoutField) o).setDateCreated(v));

    /// The modification date of a component ({@code dateModified}).
    public static final ModelProperty<String> DATE_MODIFIED = new ModelProperty<>(
        "dateModified", (o, v) -> ((AbstractExtensibleWithoutField) o).setDateModified(v));

    /// The type of a note, relation, reversal, etymology, annotation or translation.
    public static final ModelProperty<Feature> TYPE = new ModelProperty<>(
        "type", (o, v) -> ((HasType) o).setType(v));

    // Entries and senses --------------------------------------------------------------

    /// The LIFT id of an entry or a sense.
    public static final ModelProperty<String> LIFT_ID = new ModelProperty<>(
        "id", (o, v) -> ((AbstractIdentifiable) o).setId(v));

    /// The guid of an entry, a sense, a feature or a feature set.
    public static final ModelProperty<String> GUID = new ModelProperty<>(
        "guid", (o, v) -> {
            switch (o) {
                case AbstractIdentifiable i -> i.setGuid(v);
                case Feature f -> f.setGuid(v);
                case FeatureSet s -> s.setGuid(v);
                default -> throw unsupported("guid", o);
            }
        });

    /// The {@code order} attribute of an entry (a string in LIFT).
    public static final ModelProperty<String> ENTRY_ORDER = new ModelProperty<>(
        "entryOrder", (o, v) -> ((LiftEntry) o).setOrder(v));

    /// The {@code order} attribute of a sense or a relation.
    public static final ModelProperty<Integer> ORDER = new ModelProperty<>(
        "order", (o, v) -> {
            switch (o) {
                case LiftSense s -> s.setOrder(v);
                case LiftRelation r -> r.setOrder(v);
                default -> throw unsupported("order", o);
            }
        });

    /// The deletion date of an entry ({@code dateDeleted}).
    public static final ModelProperty<String> DATE_DELETED = new ModelProperty<>(
        "dateDeleted", (o, v) -> ((LiftEntry) o).setDateDeleted(v));

    /// The value (part of speech) of a grammatical info.
    public static final ModelProperty<Feature> GRAMMATICAL_INFO_VALUE = new ModelProperty<>(
        "grammaticalInfoValue", (o, v) -> ((GrammaticalInfo) o).setGramInfoValue(v));

    // References and external documents -----------------------------------------------

    /// The LIFT id a relation or a variant refers to.
    public static final ModelProperty<String> REF_ID = new ModelProperty<>(
        "ref", (o, v) -> {
            switch (o) {
                case LiftRelation r -> r.setRefId(v);
                case LiftVariant r -> r.setRefId(v);
                default -> throw unsupported("ref", o);
            }
        });

    /// The entry or sense a relation or a variant refers to.
    public static final ModelProperty<AbstractIdentifiable> REF_OBJECT = new ModelProperty<>(
        "refObject", (o, v) -> {
            switch (o) {
                case LiftRelation r -> r.setRefObject(v);
                case LiftVariant r -> r.setRefObject(v);
                default -> throw unsupported("refObject", o);
            }
        });

    /// The {@code href} of a media, an illustration or a feature set.
    public static final ModelProperty<String> HREF = new ModelProperty<>(
        "href", (o, v) -> {
            switch (o) {
                case LiftMedia m -> m.setHref(v);
                case LiftIllustration i -> i.setHref(v);
                case FeatureSet s -> s.setHref(v);
                default -> throw unsupported("href", o);
            }
        });

    /// The source of an example or an etymology.
    public static final ModelProperty<String> SOURCE = new ModelProperty<>(
        "source", (o, v) -> {
            switch (o) {
                case LiftExample e -> e.setSource(v);
                case LiftEtymology e -> e.setSource(v);
                default -> throw unsupported("source", o);
            }
        });

    // Texts, annotations, traits ------------------------------------------------------

    /// The text of a {@link Form}, with its {@code <span>} markup.
    public static final ModelProperty<String> FORM_TEXT = new ModelProperty<>(
        "text", (o, v) -> ((Form) o).changeText(v == null ? "" : v));

    /// The {@code value} attribute of an annotation.
    public static final ModelProperty<String> ANNOTATION_VALUE = new ModelProperty<>(
        "annotationValue", (o, v) -> ((LiftAnnotation) o).valueProperty().set(v));

    /// The {@code who} attribute of an annotation.
    public static final ModelProperty<String> ANNOTATION_WHO = new ModelProperty<>(
        "annotationWho", (o, v) -> ((LiftAnnotation) o).whoProperty().set(v));

    /// The {@code when} attribute of an annotation.
    public static final ModelProperty<String> ANNOTATION_WHEN = new ModelProperty<>(
        "annotationWhen", (o, v) -> ((LiftAnnotation) o).whenProperty().set(v));

    /// The value of a trait, typed after its definition's data model: a {@code String},
    /// an {@code Integer}, a {@code ZonedDateTime}, a {@link Feature}, or an unmodifiable
    /// {@code Set} or {@code List} of features (see {@link LiftTrait#getTypedValue()}).
    public static final ModelProperty<Object> TRAIT_VALUE = new ModelProperty<>(
        "traitValue", (o, v) -> ((LiftTrait) o).setTypedValue(v));

    /// What the name of a field refers to (a definition, or an undeclared name). It
    /// changes when the header declares or removes the definition of that name; set back
    /// without the header's checks, as the header itself sets it.
    public static final ModelProperty<FieldSpecification> FIELD_SPECIFICATION = new ModelProperty<>(
        "fieldSpecification", (o, v) -> ModelInternals.get().setFieldSpecification((LiftField) o, v));

    // Header ---------------------------------------------------------------------------

    /// The id of a feature (a LIFT {@code range-element}); set through
    /// {@link FeatureSet#changeFeatureId(Feature, String)}.
    public static final ModelProperty<String> FEATURE_ID = new ModelProperty<>(
        "featureId", (o, v) -> {
            Feature f = (Feature) o;
            f.getParentFeatureSet().changeFeatureId(f, v);
        });

    /// The id of a feature set (a LIFT {@code range}); set through
    /// {@link LiftHeader#renameFeatureSet(FeatureSet, String)}.
    public static final ModelProperty<String> FEATURE_SET_ID = new ModelProperty<>(
        "featureSetId", (o, v) -> {
            FeatureSet s = (FeatureSet) o;
            LiftHeader header = (LiftHeader) s.getParentNode();
            // The traits the rename rebound are reported after it, and put back by their
            // own changes.
            ModelInternals.get().withoutRebinding(header, () -> header.renameFeatureSet(s, v));
        });

    /// The parent of a feature in its feature set's hierarchy.
    public static final ModelProperty<Feature> SUPERORDINATE_FEATURE = new ModelProperty<>(
        "superordinateFeature", (o, v) -> ((Feature) o).setSuperordinateFeature(v));

    /// Whether a definition is for a field or a trait.
    public static final ModelProperty<LiftFieldAndTraitDefinitionKind> DEFINITION_KIND =
        new ModelProperty<>("definitionKind",
            (o, v) -> {
                LiftFieldAndTraitDefinition definition = (LiftFieldAndTraitDefinition) o;
                // The traits and fields the new kind rebound are put back by their own changes.
                ModelInternals.get().withoutRebinding(
                    (LiftHeader) definition.getParentNode(), () -> definition.setKind(v));
            });

    /// The components a definition applies to (an unmodifiable set).
    public static final ModelProperty<Set<LiftFieldAndTraitDefinitionTarget>> DEFINITION_TARGETS =
        new ModelProperty<>("definitionTargets",
            (o, v) -> ((LiftFieldAndTraitDefinition) o).setTargets(v));

    /// The data model a definition declares; {@code null} when it declares none.
    public static final ModelProperty<LiftFieldAndTraitDefinitionDataModel> DEFINITION_DATA_MODEL =
        new ModelProperty<>("definitionDataModel",
            (o, v) -> ((LiftFieldAndTraitDefinition) o).setDataModel(
                Optional.ofNullable(v).map(LiftFieldAndTraitDefinitionDataModel::toStringValue)));

    /// The writing systems a definition lists.
    public static final ModelProperty<String> DEFINITION_WRITING_SYSTEM = new ModelProperty<>(
        "definitionWritingSystem",
        (o, v) -> ((LiftFieldAndTraitDefinition) o).setWritingSystem(Optional.ofNullable(v)));

    /// The feature set (range) of an option definition.
    public static final ModelProperty<FeatureSet> DEFINITION_RANGE = new ModelProperty<>(
        "definitionRange",
        (o, v) -> ((LiftFieldAndTraitDefinition) o).setResolvedRange(Optional.ofNullable(v)));

    private static final List<ModelProperty<?>> ALL = List.of(
        DATE_CREATED, DATE_MODIFIED, TYPE, LIFT_ID, GUID, ENTRY_ORDER, ORDER, DATE_DELETED,
        GRAMMATICAL_INFO_VALUE, REF_ID, REF_OBJECT, HREF, SOURCE, FORM_TEXT,
        ANNOTATION_VALUE, ANNOTATION_WHO, ANNOTATION_WHEN, TRAIT_VALUE, FIELD_SPECIFICATION, FEATURE_ID,
        FEATURE_SET_ID, SUPERORDINATE_FEATURE, DEFINITION_KIND, DEFINITION_TARGETS, DEFINITION_DATA_MODEL,
        DEFINITION_WRITING_SYSTEM, DEFINITION_RANGE
    );

    private final String name;
    private final BiConsumer<Object, T> applier;

    private ModelProperty(String name, BiConsumer<Object, T> applier) {
        this.name = name;
        this.applier = applier;
    }

    /// Every constant, in declaration order.
    public static List<ModelProperty<?>> values() {
        return ALL;
    }

    /// A stable name, the same as the JavaFX property's name where there is one.
    public String name() {
        return name;
    }

    /// Set this value of {@code owner} through the public API of the model.
    ///
    /// @param owner the object holding the value
    /// @param value the value to set; {@code null} for none
    public void apply(Object owner, T value) {
        applier.accept(owner, value);
    }

    @Override
    public String toString() {
        return name;
    }

    private static IllegalArgumentException unsupported(String name, Object o) {
        return new IllegalArgumentException(
            "No '" + name + "' value on " + (o == null ? null : o.getClass().getSimpleName()));
    }
}
