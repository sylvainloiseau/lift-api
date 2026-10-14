package fr.cnrs.lacito.liftapi.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import fr.cnrs.lacito.liftapi.change.ModelProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

// between 0.13 and 0.15 : field -> field-definition, field/@tag -> field-definition/@name,
/**

 * A field-definition (LIFT {@code <field-definition>}) describes a particular field or trait type.
 *
 * A field definition gives information about a particular field type that may be used by an
application to add information not part of the LIFT standard. The goal is that data can fully
transferred between copies of the same program that reads or writes LIFT files. A different
program may or may not be able to make full use of data provided by field (or trait)
elements. (Despite its name, a field-definition may apply to a trait as well as to a field.) (lift specification : 11)
 *
 * The {@link #getKind()} method returns whether this definition
 * represents a {@link LiftFieldAndTraitDefinitionKind#FIELD} or a {@link LiftFieldAndTraitDefinitionKind#TRAIT}.
 *
 * A field definition contains :
 *
 * - name [Required, key] : This key corresponds to the name attribute found in all fields (or traits) for which this is the definition.
 *
 * - class: [Optional, string] This attribute provides the name of the LIFT element that
contains all fields (or traits) for which this is the definition. If more than
one LIFT element may contain such fields or traits, the value of class may
be a space-separated list of element names.

- type [Optional, string] This attribute defines the basic data type of the data.

  - For a
field-definition which describes a trait, the type attribute tells us about
the contents of the trait's value attribute. While any non-empty string may
be used for the type, standard values are:
• “datetime” (ISO 8601) (zero or one trait)
• “integer” (zero or one trait)
• “option” (zero or one trait)
• “option-collection” (unordered references) (zero or more traits)
• “option-sequence” (ordered references) (zero or more traits)

Note, for the “option” choices, the range referenced is specified by an
accompanying option-range attribute, described next.

  - For a field-definition which describes a field, the type attribute tells us
about the contents of the element. Standard values are:
• “multistring” (0 or more parallel strings in different writing systems,
each a single paragraph of text or less)
• “multitext” (0 or more parallel strings in different writing systems,
each possibly containing multiple paragraphs)
 *
- option-range [Optional, key] This attribute is valid only for a field-definition
that contains one of the option type values. Its value must match against an id
attribute of a range element in the header.

- writing-system [Optional, string] Provides the default list of writing systems for
displaying the information in this field. These are given as a space delimited
list of language tags (RFC 5646). This list does not limit which languages or
writing systems may contain data in the field, just which ones are displayed by
default.
Note: at this time, it is not possible to declare a multiplicity other than that implied by the
type attribute. For example, it is not currently possible to declare that a text field may be
repeated.

 * When {@code @type} is {@code option}, {@code option-collection}, or {@code option-sequence},
 * the {@code @option-range} attribute may reference a {@link FeatureSet} that enumerates
 * the allowed values. After the header is fully parsed, call
 * {@link LiftFieldAndTraitDefinition#getResolvedFeatureSet()} to link this definition to the actual range object.
 *
 *
 For trait :
 Where no range is linked the name is informal or resolved by its use in a field-definition. (12)
 *
 * @see LiftFieldAndTraitDefinitionKind
 * @see LiftFieldAndTraitDefinitionDataModel
 * @see LiftFieldAndTraitDefinitionTarget
 */
public final class LiftFieldAndTraitDefinition extends AbstractLiftRoot {

    final String name;
    final LiftHeader parent;

    // Every attribute is a tracked property: a definition is edited from the header
    // views, and there are few of them.
    private final TrackedProperty<FeatureSet> resolvedRange =
        new TrackedProperty<>(this, ModelProperty.DEFINITION_RANGE, null);
    private final TrackedProperty<String> writingSystem =
        new TrackedProperty<>(this, ModelProperty.DEFINITION_WRITING_SYSTEM, null);
    private final TrackedProperty<LiftFieldAndTraitDefinitionKind> kind =
        new TrackedProperty<>(this, ModelProperty.DEFINITION_KIND, LiftFieldAndTraitDefinitionKind.UNKNOWN);
    private final TrackedProperty<Set<LiftFieldAndTraitDefinitionTarget>> targets =
        new TrackedProperty<>(this, ModelProperty.DEFINITION_TARGETS, Set.of());
    /** The data model used to read and write values: the declared one, or STRING. */
    private LiftFieldAndTraitDefinitionDataModel definitionType = LiftFieldAndTraitDefinitionDataModel.STRING;
    /** The declared data model ({@code @type}), {@code null} when there is none. */
    private final TrackedProperty<LiftFieldAndTraitDefinitionDataModel> declaredDataModel =
        new TrackedProperty<>(this, ModelProperty.DEFINITION_DATA_MODEL, null);

    MultiText label = new MultiText(this);

    /**
     * Creates a definition for the given header. Use the header's {@code create...}
     * methods, which also add it there.
     */
    public LiftFieldAndTraitDefinition(String name, LiftHeader parent) {
        this.name = name;
        this.parent = parent;
    }

    public Optional<FeatureSet> getResolvedFeatureSet() {
        return Optional.ofNullable(resolvedRange.get());
    }

    public void setResolvedRange(Optional<FeatureSet> resolvedRange) {
        this.resolvedRange.set(resolvedRange == null ? null : resolvedRange.orElse(null));
    }

    /** The range of an option definition; {@code null} when there is none. */
    public ObjectProperty<FeatureSet> resolvedRangeProperty() {
        return resolvedRange;
    }

    public Optional<String> getWritingSystem() {
        return Optional.ofNullable(writingSystem.get());
    }

    public void setWritingSystem(Optional<String> writingSystem) {
        this.writingSystem.set(writingSystem == null ? null : writingSystem.orElse(null));
    }

    /** The writing systems listed; {@code null} when there are none. */
    public ObjectProperty<String> writingSystemProperty() {
        return writingSystem;
    }

    public MultiText getLabel() {
        return label;
    }

    public void setKind(LiftFieldAndTraitDefinitionKind kind) {
        this.kind.set(kind);
    }

    public LiftFieldAndTraitDefinitionKind getKind() {
        return kind.get();
    }

    /** Whether this definition is for fields, traits, or not known yet. */
    public ObjectProperty<LiftFieldAndTraitDefinitionKind> kindProperty() {
        return kind;
    }

    /**
     * The data model used to read and write the values: the declared one, or
     * {@link LiftFieldAndTraitDefinitionDataModel#STRING} when none is declared.
     */
    public Optional<LiftFieldAndTraitDefinitionDataModel> getDataModel() {
        return Optional.of(definitionType);
    }

    /**
     * @deprecated use {@link #setDataModel(Optional)}, which also records that the data
     *             model is declared
     */
    @Deprecated(forRemoval = true)
    public void setDefinitionType(Optional<LiftFieldAndTraitDefinitionDataModel> definitionType) {
        setDataModel(definitionType.map(LiftFieldAndTraitDefinitionDataModel::toStringValue));
    }

    public String getName() {
        return name;
    }

    public MultiText getDescription() {
        return getMainMultiText();
    }

    /**
     * @return the components this definition applies to, an unmodifiable set
     */
    public Set<LiftFieldAndTraitDefinitionTarget> getTargets() {
        return targets.get();
    }

    /** The components this definition applies to, read-only: see {@link #setTargets(Set)}. */
    public ReadOnlyObjectProperty<Set<LiftFieldAndTraitDefinitionTarget>> targetsProperty() {
        return targets.getReadOnlyProperty();
    }

    public String getTargetAsString() {
        return getTargets().stream().map(LiftFieldAndTraitDefinitionTarget::toStringValue)
            .collect(Collectors.joining(" "));
    }

    /** Set the targets from the space-separated list of the {@code class} attribute. */
    public void setTargets(String targetString) {
        setTargets(LiftFieldAndTraitDefinitionTarget.parseTargetString(targetString));
    }

    /**
     * @param targets the components this definition applies to; {@code null} for none
     */
    public void setTargets(Set<LiftFieldAndTraitDefinitionTarget> targets) {
        Set<LiftFieldAndTraitDefinitionTarget> copy = targets == null || targets.isEmpty()
            ? Set.of()
            : Collections.unmodifiableSet(EnumSet.copyOf(targets));
        this.targets.set(copy);
    }

    public Optional<LiftFieldAndTraitDefinitionDataModel> getType() {
        return getDataModel();
    }

    public Optional<String> getTypeStr() {
        return getDataModel().map(LiftFieldAndTraitDefinitionDataModel::toStringValue);
    }

    /** The declared {@code @type}, empty when none was declared. */
    public Optional<String> getDeclaredTypeStr() {
        return isDataModelDeclared() ? getTypeStr() : Optional.empty();
    }

    /** Whether a {@code @type} was explicitly declared for this definition. */
    public boolean isDataModelDeclared() {
        return declaredDataModel.get() != null;
    }

    /** The declared data model, read-only: see {@link #setDataModel(Optional)}. */
    public ReadOnlyObjectProperty<LiftFieldAndTraitDefinitionDataModel> declaredDataModelProperty() {
        return declaredDataModel.getReadOnlyProperty();
    }

    /**
     * Set the declared data model from the raw {@code @type} string. An unknown or absent
     * type leaves the definition undeclared, read and written as a string. A definition
     * whose kind is still unknown takes the kind the data model implies.
     *
     * @throws IllegalStateException if the data model would change while fields or traits
     *         use this definition
     *
     * @param typeStr the {@code @type} value, or empty
     */
    public void setDataModel(Optional<String> typeStr) {
        LiftFieldAndTraitDefinitionDataModel declared = typeStr == null ? null : typeStr
            .flatMap(LiftFieldAndTraitDefinitionDataModel::fromStringValue)
            .orElse(null);
        LiftFieldAndTraitDefinitionDataModel effective =
            declared == null ? LiftFieldAndTraitDefinitionDataModel.STRING : declared;
        if (effective != definitionType && parent != null && parent.isUsed(this)) {
            // The values of its traits are held in properties of the old data model.
            throw new IllegalStateException(
                "The data model of the definition " + name + " cannot change: fields or traits use it.");
        }
        this.definitionType = effective;
        this.declaredDataModel.set(declared);
        if (getKind() == LiftFieldAndTraitDefinitionKind.UNKNOWN && declared != null) {
            setKind(LiftFieldAndTraitDefinitionKind.fromType(declared));
        }
    }

    public boolean isFieldDefinition() {
        return getKind() == LiftFieldAndTraitDefinitionKind.FIELD;
    }

    public boolean isTraitDefinition() {
        return getKind() == LiftFieldAndTraitDefinitionKind.TRAIT;
    }

    @Override
    public String toString() {
        return "LiftFieldAndTraitDefinition{" +
            "name='" + name + '\'' +
            ", kind=" + getKind() +
            ", targets=" + getTargetAsString() +
            ", definitionType=" + definitionType +
            '}';
    }

    @Override
    public AbstractLiftRoot getParentNode() {
        return parent;
    }
}
